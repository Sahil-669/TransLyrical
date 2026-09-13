package com.example.translyrical.domain

import android.util.Log
import com.example.translyrical.BuildConfig
import com.example.translyrical.network.GeminiApi
import com.example.translyrical.network.GeminiContent
import com.example.translyrical.network.GeminiPart
import com.example.translyrical.network.GeminiRequest
import com.example.translyrical.parser.LyricLine
import com.google.gson.Gson


data class GeminiTranslationResponse(
    val english: List<String>?,
    val hindi: List<String>?
)

data class MultiLangTranslation(
    val english: List<LyricLine>?,
    val hindi: List<LyricLine>?
)

class LyricTranslator (
    private val geminiApi: GeminiApi
) {
    private val gson = Gson()
    suspend fun getMultiLangTranslation(originalLyrics: List<LyricLine>): MultiLangTranslation? {
        if (originalLyrics.isEmpty()) return null

        val rawLyricsText = originalLyrics.joinToString("\n") { it.text }

        val promptText = """
            You are a professional music translator. Translate the following lyrics into both English and Hindi.
            Return ONLY a valid JSON object with two arrays of strings, matching the exact number of lines provided.
            Do not include markdown blocks or any other text.
            Format:
            {
              "english": ["line 1 translated", "line 2 translated"],
              "hindi": ["लाइन 1 का अनुवाद", "लाइन 2 का अनुवाद"]
            }
            
            Lyrics to translate:
            $rawLyricsText
        """.trimIndent()
        val request = GeminiRequest(
            contents = listOf(GeminiContent(listOf(GeminiPart(promptText))))
        )
        return try {
            val response = geminiApi.translateLyrics(BuildConfig.GEMINI_API_KEY, request)
            var responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText.isNullOrBlank()) return null
            responseText = responseText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsedJson = gson.fromJson(responseText, GeminiTranslationResponse::class.java)
            val englishLines = parsedJson.english?.mapIndexed { index, text ->
                LyricLine(
                    startTimeMs = originalLyrics.getOrNull(index)?.startTimeMs ?: 0L,
                    text = text
                )
            }

            val hindiLines = parsedJson.hindi?.mapIndexed { index, text ->
                LyricLine(
                    startTimeMs = originalLyrics.getOrNull(index)?.startTimeMs ?: 0L,
                    text = text
                )
            }

            MultiLangTranslation(englishLines, hindiLines)
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("LyricTranslator", "Gemini HTTP ${e.code()}: $errorBody")
            null
        } catch (e: Exception) {
            Log.e("LyricTranslator", "Gemini API failed", e)
            null
        }
    }
}
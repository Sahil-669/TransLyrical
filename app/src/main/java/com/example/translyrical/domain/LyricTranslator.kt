package com.example.translyrical.domain

import android.util.Log
import com.example.translyrical.BuildConfig
import com.example.translyrical.network.GeminiApi
import com.example.translyrical.network.GeminiContent
import com.example.translyrical.network.GeminiPart
import com.example.translyrical.network.GeminiRequest
import com.example.translyrical.parser.LyricLine

class LyricTranslator (
    private val geminiApi: GeminiApi
) {
    suspend fun getFullSongTranslation(originalLyrics: List<LyricLine>): List<LyricLine>? {
        if (originalLyrics.isEmpty()) return null

        val rawLyricsText = originalLyrics.mapIndexed { index, line ->
            "$index| ${line.text}"
        }.joinToString("\n")

        val promptText = """
            Translate the following song lyrics into English.
            Rules:
            1. The text may be in Romanized Hindi (Hinglish), standard Spanish, slang, or mixed languages.
            2. I have numbered each line (e.g., "0| text"). You MUST return the exact same line numbers in your response.
            3. If a line is empty, return just the line number and a pipe (e.g., "5| ").
            4. Return ONLY the numbered translations. No introductions.
            
            Lyrics:
            $rawLyricsText
        """.trimIndent()
        val request = GeminiRequest(
            contents = listOf(GeminiContent(listOf(GeminiPart(promptText))))
        )
        return try {
            val response = geminiApi.translateLyrics(BuildConfig.GEMINI_API_KEY, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText.isNullOrBlank()) return null
            val translatedLines = responseText.trim().split("\n")
                .mapNotNull { line ->
                    val parts = line.split("|", limit = 2)
                    if (parts.size == 2) {
                        val index = parts[0].trim().toIntOrNull()
                        val text = parts[1].trim()
                        if (index != null) index to text else null
                    } else {
                        null
                    }
                }.toMap()

            originalLyrics.mapIndexed { index, originalLine ->
                val englishText = translatedLines[index] ?: originalLine.text
                LyricLine(
                    startTimeMs = originalLine.startTimeMs,
                    text = englishText
                )
            }
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
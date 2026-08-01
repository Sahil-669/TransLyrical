package com.example.translyrical.domain

import com.example.translyrical.network.TranslationApi
import com.example.translyrical.parser.LyricLine


class LyricTranslator (
    private val api: TranslationApi,
) {
    suspend fun getFullSongTranslation(
        originalLyrics: List<LyricLine>
    ) : List<LyricLine> {

        val finalTranslatedList = mutableListOf<LyricLine>()
        val chunks = originalLyrics.chunked(8)

        for (chunk in chunks) {
            val bulkText = chunk.joinToString("@@@") { line -> line.text }

            try {
                val response = api.translateText(textToTranslate = bulkText)
                val bulkTranslatedText = response.get(0).asJsonArray.get(0).asJsonArray.get(0).asString

                val translatedStrings = bulkTranslatedText.split(Regex("\\s*@@@\\s*"))
                chunk.forEachIndexed { index, originalLine ->
                    val enText = translatedStrings.getOrNull(index)?.trim() ?: originalLine.text
                    finalTranslatedList.add(LyricLine(originalLine.startTimeMs, enText))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finalTranslatedList.addAll(chunk)
            }
        }
        return finalTranslatedList
    }
}
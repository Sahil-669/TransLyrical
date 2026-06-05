package com.example.translyrical.domain

import com.example.translyrical.data.local.SongCacheDao
import com.example.translyrical.data.local.SongCacheEntity
import com.example.translyrical.data.repository.DictionaryRepository
import com.example.translyrical.network.TranslationApi
import com.example.translyrical.parser.LyricLine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class LyricTranslator (
    private val repository: DictionaryRepository,
    private val api: TranslationApi,
    private val cacheDao: SongCacheDao
) {
    private val gson = Gson()
    suspend fun getFullSongTranslation(
        songId: String,
        originalLyrics: List<LyricLine>
    ) : List<LyricLine> {

        val cached = cacheDao.getTranslationCache(songId)
        if (cached != null) {
            val listType = object : TypeToken<List<LyricLine>>() {}.type
            return gson.fromJson(cached.translatedJson, listType)
        }

        repository.syncDictionaryIfEmpty()
        val finalTranslatedList = mutableListOf<LyricLine>()
        val chunks = originalLyrics.chunked(8)

        for (chunk in chunks) {
            val bulkText = chunk.map { line ->
                normalizeLine(line.text)
            }.joinToString("@@@")

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

        val jsonToSave = gson.toJson(finalTranslatedList)
        cacheDao.insertCache(SongCacheEntity(songId, jsonToSave))

        return finalTranslatedList
    }

    private suspend fun normalizeLine(rawLyric: String): String {
        val tokens = rawLyric.split(Regex("\\b"))
        return tokens.map { token ->
            if (token.matches(Regex("\\p{L}+"))) {
                repository.getStandardTranslation(token.lowercase(Locale.getDefault())) ?: token
            } else {
                token
            }
        }.joinToString("")
    }

}
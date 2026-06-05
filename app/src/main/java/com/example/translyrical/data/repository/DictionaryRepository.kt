package com.example.translyrical.data.repository

import android.content.Context
import com.example.translyrical.data.local.SlangDao
import com.example.translyrical.data.local.SlangEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryRepository (
    private val context: Context,
    private val slangDao: SlangDao
) {
    suspend fun syncDictionaryIfEmpty() {
        withContext(Dispatchers.IO) {
            val currentSize = slangDao.getDictionarySize()

            if (currentSize == 0) {
                val jsonString = context.assets.open("slang_dict.json")
                    .bufferedReader()
                    .use { it.readText() }

                val listType = object : TypeToken<List<SlangEntity>>() {}.type
                val slangList: List<SlangEntity> = Gson().fromJson(jsonString, listType)

                slangDao.insertAll(slangList)
            }
        }
    }

    suspend fun getStandardTranslation(slang: String): String? {
        return withContext(Dispatchers.IO) {
            slangDao.getStandardWord(slang)
        }
    }
}
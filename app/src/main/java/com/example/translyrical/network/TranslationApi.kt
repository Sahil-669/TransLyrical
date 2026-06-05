package com.example.translyrical.network

import com.google.gson.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationApi {

    @GET("translate_a/single")
    suspend fun translateText(
        @Query("client") client: String = "gtx",
        @Query("sl") sourceLang: String = "es",
        @Query("tl") targetLang: String = "en",
        @Query("dt") dt: String = "t",
        @Query("q") textToTranslate: String,
    ): JsonArray
}
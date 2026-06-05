package com.example.translyrical.network

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SpotifySearchApi {
    @GET("v1/search")
    suspend fun searchTrack(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 1
        ) : JsonObject
}
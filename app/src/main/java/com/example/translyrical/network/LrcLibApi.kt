package com.example.translyrical.network


import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): LrcLibResponse
}

data class LrcLibResponse(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val syncedLyrics: String?
)
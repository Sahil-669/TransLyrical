package com.example.translyrical.network


import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): LrcLibResponse

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrcLibResponse>
}

data class LrcLibResponse(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val duration: Double?,
    val syncedLyrics: String?
)
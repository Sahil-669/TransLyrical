package com.example.translyrical.network

import retrofit2.http.GET
import retrofit2.http.Query

data class ITunesResponse(val results: List<ITunesTrack>)
data class ITunesTrack(
    val trackName: String,
    val artistName: String,
    val artworkUrl100: String
)

interface ITunesApi {
    @GET("search?entity=song&limit=25")
    suspend fun getArtistTracks(
        @Query("term") artistName: String
    ): ITunesResponse
}
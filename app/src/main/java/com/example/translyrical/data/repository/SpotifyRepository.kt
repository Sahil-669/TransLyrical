package com.example.translyrical.data.repository

import android.util.Base64
import com.example.translyrical.network.SpotifyAuthApi
import com.example.translyrical.network.SpotifySearchApi
import com.example.translyrical.BuildConfig

class SpotifyRepository (
    private val authApi: SpotifyAuthApi,
    private val searchApi: SpotifySearchApi
) {
    private var cachedToken: String? = null

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET

    private suspend fun getValidToken(): String {
        cachedToken?.let { return it }

        val secrets = "$clientId:$clientSecret"
        val base64Auth = "Basic " + Base64.encodeToString(secrets.toByteArray(), Base64.NO_WRAP)
        val response = authApi.getAccessToken(base64Auth)
        cachedToken = "Bearer ${response.access_token}"
        return cachedToken!!
    }

    suspend fun fetchCoverArtAndMeta(rawQuery: String): SpotifyMetadata? {
        return try {
            val token = getValidToken()
            val cleanedQuery = cleanFilename(rawQuery)
            val jsonResponse = searchApi.searchTrack(token, cleanedQuery)
            val trackObject = jsonResponse.getAsJsonObject("tracks")
                ?.getAsJsonArray("items")?.firstOrNull()?.asJsonObject ?: return null

            val title = trackObject.getAsJsonPrimitive("name").asString
            val artist = trackObject.getAsJsonArray("artists")
                ?.firstOrNull()?.asJsonObject?.getAsJsonPrimitive("name")?.asString ?: "Unknown Artist"
            val coverUrl = trackObject.getAsJsonObject("album")?.getAsJsonArray("images")
                ?.firstOrNull()?.asJsonObject?.getAsJsonPrimitive("url")?.asString
            val spotifyId = trackObject.getAsJsonPrimitive("id").asString

            SpotifyMetadata(title = title, artist = artist, coverArtUrl = coverUrl, spotifyId = spotifyId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun cleanFilename(filename: String): String {
        return filename
            .replace(Regex("(?i)\\.mp3"), "")
            .replace(Regex("(?i)official\\s*video"), "")
            .replace(Regex("(?i)official\\s*audio"), "")
            .replace(Regex("(?i)y2mate\\.com\\s*-?\\s*"), "")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }
}

data class SpotifyMetadata(val title: String, val artist: String, val coverArtUrl: String?, val spotifyId: String? = null)
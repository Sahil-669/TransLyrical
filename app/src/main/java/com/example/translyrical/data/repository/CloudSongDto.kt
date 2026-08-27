package com.example.translyrical.data.repository

import com.example.translyrical.domain.CloudSong
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudSongDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("youtube_id")
    val youtubeId: String? = null,
    @SerialName("title")
    val title: String = "",
    @SerialName("artist")
    val artist: String = "",
    @SerialName("cover_url")
    val coverUrl: String? = null,
    @SerialName("synced_lyrics_json")
    val syncedLyricsJson: String? = null,
    @SerialName("translated_lyrics_json")
    val translatedLyricsJson: String? = null,
    @SerialName("timestamp")
val timestamp: Long = 0L
) {
    fun toDomain(): CloudSong {
        return CloudSong(
            id = id?: "",
            youtubeId= youtubeId,
            title = title,
            artist = artist,
            coverUrl = coverUrl,
            syncedLyricsJson = syncedLyricsJson,
            translatedLyricsJson = translatedLyricsJson,
            timestamp = timestamp
        )
    }
}

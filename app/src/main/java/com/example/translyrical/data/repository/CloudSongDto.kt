package com.example.translyrical.data.repository

import com.example.translyrical.domain.CloudSong

data class CloudSongDto(
    val title: String = "",
    val artist: String = "",
    val audioUrl: String = "",
    val timestamp: Long = 0L
) {
    fun toDomain(documentId: String): CloudSong {
        return CloudSong(
            id = documentId,
            title = title,
            artist = artist,
            audioUrl = audioUrl,
            timestamp = timestamp
        )
    }
}

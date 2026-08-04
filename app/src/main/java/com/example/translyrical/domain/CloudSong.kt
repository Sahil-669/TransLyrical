package com.example.translyrical.domain

data class CloudSong(
    val id: String,
    val spotifyId: String?,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String?,
    val syncedLyricsJson: String?,
    val translatedLyricsJson: String?,
    val timestamp: Long
)

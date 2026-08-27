package com.example.translyrical.domain

data class CloudSong(
    val id: String,
    val youtubeId: String?,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val syncedLyricsJson: String?,
    val translatedLyricsJson: String?,
    val timestamp: Long
)

package com.example.translyrical.domain

data class CloudSong(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val timestamp: Long
)

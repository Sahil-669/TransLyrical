package com.example.translyrical.data.repository

import com.example.translyrical.domain.CloudSong

interface CloudSongRepository {
    suspend fun uploadCloudSong(title: String, artist: String, audioBytes: ByteArray, coverUrl: String?, syncedLyricsJson: String?, translatedLyricsJson: String?): Result<Unit>
    suspend fun getCloudSongs(): Result<List<CloudSong>>
}
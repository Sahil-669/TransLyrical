package com.example.translyrical.data.repository

import com.example.translyrical.domain.CloudSong

interface CloudSongRepository {
    suspend fun uploadCloudSong(spotifyId: String?,title: String, artist: String, audioBytes: ByteArray, coverUrl: String?, syncedLyricsJson: String?, translatedLyricsJson: String?): Result<Unit>
    suspend fun getCloudSongs(): Result<List<CloudSong>>
    suspend fun getExistingSong(spotifyId: String?, title: String, artist: String): CloudSong?
    suspend fun deleteSong(id: String)
}
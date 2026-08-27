package com.example.translyrical.data.repository

import com.example.translyrical.domain.CloudSong

interface CloudSongRepository {
    suspend fun uploadCloudSong(youtubeId: String?,title: String, artist: String, coverUrl: String?, syncedLyricsJson: String?, translatedLyricsJson: String?): Result<Unit>
    suspend fun getCloudSongs(): Result<List<CloudSong>>
    suspend fun getExistingSong(youtubeId: String?, title: String, artist: String): CloudSong?
    suspend fun deleteSong(id: String)
}
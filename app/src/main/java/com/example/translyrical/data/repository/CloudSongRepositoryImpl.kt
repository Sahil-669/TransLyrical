package com.example.translyrical.data.repository

import android.util.Log
import com.example.translyrical.domain.CloudSong
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class CloudSongRepositoryImpl(
    private val supabase: SupabaseClient
) : CloudSongRepository{

    override suspend fun uploadCloudSong(
        youtubeId: String?,
        title: String,
        artist: String,
        coverUrl: String?,
        syncedLyricsJson: String?,
        translatedLyricsJson: String?
    ): Result<Unit> {
        return try {
            val songDto = CloudSongDto(
                youtubeId = youtubeId,
                title = title,
                artist = artist,
                coverUrl = coverUrl,
                syncedLyricsJson = syncedLyricsJson,
                translatedLyricsJson = translatedLyricsJson,
                timestamp = System.currentTimeMillis()
            )
            supabase.postgrest["songs"].insert(songDto)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseUpload", "Pipeline failure: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getCloudSongs(): Result<List<CloudSong>> {
        return try {
            val dtos = supabase.postgrest["songs"]
                .select()
                .decodeList<CloudSongDto>()

            val songs = dtos.map { it.toDomain() }

            Result.success(songs)
        } catch (e: Exception) {
            Log.e("SupabaseFetch", "Failed to fetch library: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getExistingSong(
        youtubeId: String?,
        title: String,
        artist: String
    ): CloudSong? {
        return try {
            if (!youtubeId.isNullOrBlank()) {
                val songBySpotify = supabase.postgrest["songs"]
                    .select {
                        filter { eq("youtube_id", youtubeId) }
                    }
                    .decodeSingleOrNull<CloudSongDto>()
                if (songBySpotify != null) {
                    return songBySpotify.toDomain()
                }
            }
            val fallbackResults = supabase.postgrest["songs"]
                .select {
                    filter {
                        eq("title", title)
                        eq("artist", artist)
                    }
                }
                .decodeList<CloudSongDto>()
            return fallbackResults.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e("CloudSongRepo", "Failed to check for existing song", e)
            null
        }
    }

    override suspend fun deleteSong(id: String) {
        try {
            supabase.postgrest["songs"].delete {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            Log.e("CloudSongRepo", "Failed to delete song", e)
            throw e
        }
    }
}
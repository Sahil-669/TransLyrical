package com.example.translyrical.data.repository

import android.util.Log
import com.example.translyrical.domain.CloudSong
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import java.util.UUID

class CloudSongRepositoryImpl(
    private val supabase: SupabaseClient
) : CloudSongRepository{

    override suspend fun uploadCloudSong(
        spotifyId: String?,
        title: String,
        artist: String,
        audioBytes: ByteArray,
        coverUrl: String?,
        syncedLyricsJson: String?,
        translatedLyricsJson: String?
    ): Result<Unit> {
        return try {
            val fileName = "${UUID.randomUUID()}.mp3"
            supabase.storage["songs"].upload(fileName, audioBytes) {
                upsert = false
            }

            val publicAudioUrl = supabase.storage["songs"].publicUrl(fileName)

            val songDto = CloudSongDto(
                spotifyId = spotifyId,
                title = title,
                artist = artist,
                audioUrl = publicAudioUrl,
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
        spotifyId: String?,
        title: String,
        artist: String
    ): CloudSong? {
        return try {
            if (!spotifyId.isNullOrBlank()) {
                val songBySpotify = supabase.postgrest["songs"]
                    .select {
                        filter { eq("spotify_id", spotifyId) }
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
            val songToDelete = supabase.postgrest["songs"]
                .select { filter { eq("id", id) } }
                .decodeSingleOrNull<CloudSongDto>()
            if (songToDelete != null) {
                val fileName = songToDelete.audioUrl.substringAfterLast("/")
                try {
                    supabase.storage["songs"].delete(fileName)
                } catch (e: Exception) {
                    Log.e("CloudSongRepo", "Failed to delete storage file, it may be orphaned", e)
                }
                supabase.postgrest["songs"].delete {
                    filter { eq("id", id) }
                }
            }
        } catch (e: Exception) {
            Log.e("CloudSongRepo", "Failed to delete song", e)
            throw e
        }
    }
}
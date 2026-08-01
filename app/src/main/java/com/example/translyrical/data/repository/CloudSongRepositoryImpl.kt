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
}
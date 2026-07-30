package com.example.translyrical.data.repository

import com.example.translyrical.domain.CloudSong
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudSongRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val supabaseClient: SupabaseClient
) : CloudSongRepository{
    private val collectionRef = firestore.collection("cloud_songs")

    override suspend fun uploadCloudSong(
        title: String,
        artist: String,
        audioBytes: ByteArray
    ): Result<Unit> {
        return try {
            val fileName = "${UUID.randomUUID()}.mp3"

            val bucket = supabaseClient.storage.from("songs")
            bucket.upload(path = fileName, data = audioBytes)

            val publicAudioUrl = bucket.publicUrl(fileName)

            val cloudSongDto = CloudSongDto(
                title = title,
                artist = artist,
                audioUrl = publicAudioUrl,
                timestamp = System.currentTimeMillis()
            )
            collectionRef.add(cloudSongDto).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getCloudSongs(): Result<List<CloudSong>> {
        return try {
            val snapshot = collectionRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val cloudSongs = snapshot.documents.mapNotNull { document ->
                val dto = document.toObject(CloudSongDto::class.java)
                dto?.toDomain(documentId = document.id)
            }
            Result.success(cloudSongs)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
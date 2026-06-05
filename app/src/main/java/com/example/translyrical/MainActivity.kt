package com.example.translyrical

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.domain.LyricTranslator
import com.example.translyrical.network.LrcLibApi
import com.example.translyrical.parser.LrcParser
import com.example.translyrical.parser.LyricLine
import com.example.translyrical.player.rememberLyricPlayer
import com.example.translyrical.ui.LyricScreen
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val lyricTranslator = koinInject<LyricTranslator>()
            val lrcLibApi = koinInject<LrcLibApi>()
            val spotifyRepository = koinInject<SpotifyRepository>()

            var audioUri by remember { mutableStateOf<Uri?>(null) }
            var lyricsList by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
            var translatedLyrics by remember { mutableStateOf<List<LyricLine>?>(null) }
            var isFetching by remember { mutableStateOf(false) }
            var currentTitle by remember { mutableStateOf("Unknown Track") }
            var currentArtist by remember { mutableStateOf("Unknown Artist") }
            var currentCover by remember { mutableStateOf<String?>(null) }

            val audioPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    isFetching = true
                    audioUri = uri
                    lyricsList = emptyList()
                    translatedLyrics = null
                    currentCover = null
                }
            }

            LaunchedEffect(audioUri) {
                if (audioUri == null) return@LaunchedEffect

                isFetching = true
                try {
                    val localMeta = extractMetadata(context, audioUri!!)
                    val searchString = localMeta?.let { "${it.title} ${it.artist}" }
                        ?: audioUri!!.lastPathSegment
                        ?: ""

                    val spotifyMeta = spotifyRepository.fetchCoverArtAndMeta(searchString)
                    currentTitle = spotifyMeta?.title?: localMeta?.title?: audioUri!!.lastPathSegment?: "Unknown Track"
                    currentArtist = spotifyMeta?.artist ?: localMeta?.artist ?: "Unknown Artist"
                    currentCover = spotifyMeta?.coverArtUrl

                    val response = lrcLibApi.getLyrics(currentTitle, currentArtist)
                    if (response.syncedLyrics != null) {

                        lyricsList = LrcParser.parse(response.syncedLyrics)

                        val uniqueId = "${currentTitle}_${currentArtist}".replace(" ", "_").lowercase()
                        translatedLyrics = lyricTranslator.getFullSongTranslation(
                                uniqueId,
                                lyricsList
                        )
                    }

                } catch (e: Exception) {
                    print("❌ Fetch Pipeline Failed: ${e.message}")
                } finally {
                    isFetching = false
                }
            }

            if (audioUri == null) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        }
                    ) {
                        Text("Select MP3 file")
                    }
                }
            } else if (lyricsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isFetching) {
                            Text("Fetching Synced Lyrics...", color = Color.White)
                        } else {
                            Text(
                                "No lyrics found online.",
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            } else {
                val playerState = rememberLyricPlayer(
                    lyricsList,
                    audioUri
                )

                LyricScreen(
                    playerState,
                    translatedLyrics,
                    currentTitle,
                    currentArtist,
                    currentCover
                )
            }
        }
    }
}

data class SongMetadata(
    val title: String,
    val artist: String
)

fun extractMetadata(context: Context, uri: Uri): SongMetadata? {

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            SongMetadata(title, artist)
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}


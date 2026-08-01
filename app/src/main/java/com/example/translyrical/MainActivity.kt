package com.example.translyrical

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.domain.CloudSong
import com.example.translyrical.domain.LyricTranslator
import com.example.translyrical.network.LrcLibApi
import com.example.translyrical.parser.LrcParser
import com.example.translyrical.parser.LyricLine
import com.example.translyrical.player.rememberLyricPlayer
import com.example.translyrical.ui.CloudSongViewModel
import com.example.translyrical.ui.LyricScreen
import com.example.translyrical.ui.RippleBackground
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.core.net.toUri
import coil3.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransLyrical()
        }
    }
}

@Composable
fun TransLyrical() {
    val context = LocalContext.current
    val lyricTranslator = koinInject<LyricTranslator>()
    val lrcLibApi = koinInject<LrcLibApi>()
    val spotifyRepository = koinInject<SpotifyRepository>()
    val cloudSongViewModel = koinViewModel<CloudSongViewModel>()
    val uiState by cloudSongViewModel.uiState.collectAsState()

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var lyricsList by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var translatedLyrics by remember { mutableStateOf<List<LyricLine>?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Unknown Track") }
    var currentArtist by remember { mutableStateOf("Unknown Artist") }
    var currentCover by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(audioUri) {
        if (audioUri == null) return@LaunchedEffect
        if (audioUri!!.scheme?.startsWith("http") == true) return@LaunchedEffect

        isFetching = true
        try {
            val localMeta = extractMetadata(context, audioUri!!)

            val tempTitle = localMeta?.title?: audioUri!!.lastPathSegment ?: "Unknown Track"
            val tempArtist = localMeta?.artist ?: "Unknown Artist"
            val existingSong = uiState.songs.find { cloudSong ->
                cloudSong.title.equals(tempTitle, ignoreCase = true) &&
                        cloudSong.artist.equals(tempArtist, ignoreCase = true)
            }
            if (existingSong != null) {
                lyricsList = existingSong.syncedLyricsJson.toLyricsList()
                translatedLyrics = existingSong.translatedLyricsJson.toLyricsList()
                currentTitle = existingSong.title
                currentArtist = existingSong.artist
                currentCover = existingSong.coverUrl
                isFetching = false
                Log.d("TransLyrical", "Song already in library, skipping upload.")
                return@LaunchedEffect
            }

            val searchString = localMeta?.let { "${it.title} ${it.artist}" }
                ?: audioUri!!.lastPathSegment
                ?: ""

            val spotifyMeta = spotifyRepository.fetchCoverArtAndMeta(searchString)
            currentTitle = spotifyMeta?.title ?: localMeta?.title ?: audioUri!!.lastPathSegment ?: "Unknown Track"
            currentArtist = spotifyMeta?.artist ?: localMeta?.artist ?: "Unknown Artist"
            currentCover = spotifyMeta?.coverArtUrl

            val response = lrcLibApi.getLyrics(currentTitle, currentArtist)
            if (response.syncedLyrics != null) {
                lyricsList = LrcParser.parse(response.syncedLyrics)
                translatedLyrics = lyricTranslator.getFullSongTranslation(lyricsList)

                val audioBytes = context.contentResolver.openInputStream(audioUri!!)?.use { inputStream ->
                    inputStream.readBytes()
                }

                if (audioBytes != null) {
                    cloudSongViewModel.uploadSong(
                        title = currentTitle,
                        artist = currentArtist,
                        audioBytes = audioBytes,
                        coverUrl = currentCover,
                        syncedLyrics = lyricsList,
                        translatedLyrics = translatedLyrics
                    )
                } else {
                    Log.e("TransLyricalFetch", "Failed to extract bytes from URI: $audioUri")
                }
            }
        } catch (e: Exception) {
            Log.e("TransLyricalFetch", "Pipeline critical failure", e)
        } finally {
            isFetching = false
        }
    }

    RippleBackground(iconRes = R.drawable.ic_music_note) {
        if (audioUri == null) {
            MainScreen(
                cloudViewModel = cloudSongViewModel,
                onAudioSelected = { selectedUri ->
                    isFetching = true
                    audioUri = selectedUri
                    lyricsList = emptyList()
                    translatedLyrics = null
                    currentCover = null
                },
                onCloudSongSelected = { cloudSong ->
                    audioUri = cloudSong.audioUrl.toUri()
                    lyricsList = cloudSong.syncedLyricsJson.toLyricsList()
                    translatedLyrics = cloudSong.translatedLyricsJson.toLyricsList()
                    currentTitle = cloudSong.title
                    currentArtist = cloudSong.artist
                    currentCover = cloudSong.coverUrl
                    isFetching = false
                }
            )
        } else if (lyricsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isFetching) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching Synced Lyrics...", color = Color.White)
                    } else {
                        Text(
                            "No lyrics found online.",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = { audioUri = null }) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else {
            val playerState = rememberLyricPlayer(lyricsList, audioUri)
            LyricScreen(playerState, translatedLyrics, currentTitle, currentArtist, currentCover)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    cloudViewModel: CloudSongViewModel,
    onAudioSelected: (Uri) -> Unit,
    onCloudSongSelected: (CloudSong) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (pagerState.currentPage == 1) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Song")
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> AddSongsScreen(onAudioSelected)
                1 -> LibraryScreen(cloudViewModel, onCloudSongSelected)
            }
        }
    }
}

@Composable
fun AddSongsScreen(onAudioSelected: (Uri) -> Unit) {
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onAudioSelected(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = { audioPickerLauncher.launch("audio/*") },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(.25f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp)
        ) {
            Text("Select MP3", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: CloudSongViewModel,
    onCloudSongSelected: (CloudSong) -> Unit
    ) {

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp, start = 20.dp, end = 20.dp)
    ) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp, start = 4.dp)
        )

        if (uiState.isLoading && uiState.songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your library is empty.\nSwipe left or click the + button to add a song!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else if (uiState.songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your library is empty.\nSwipe left or click the + button to add a song!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn( verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(uiState.songs) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onCloudSongSelected(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongListItem(song: CloudSong, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (song.coverUrl != null) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = "Album art for ${song.title}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Play",
            tint = Color.White.copy(alpha = 0.8f)
        )
    }
}

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

data class SongMetadata(val title: String, val artist: String)

fun String?.toLyricsList(): List<LyricLine> {
    if (this.isNullOrBlank()) return emptyList()
    return try {
        val listType = object : TypeToken<List<LyricLine>>() {}.type
        Gson().fromJson(this, listType)
    } catch (e: Exception) {
        Log.e("TransLyricalParse", "Failed to deserialize lyrics", e)
        emptyList()
    }
}
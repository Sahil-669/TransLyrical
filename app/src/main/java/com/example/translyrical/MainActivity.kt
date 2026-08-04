package com.example.translyrical

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.translyrical.network.LrcLibResponse
import retrofit2.HttpException

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
    val navController = rememberNavController()

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var lyricsList by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var translatedLyrics by remember { mutableStateOf<List<LyricLine>?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Unknown Track") }
    var currentArtist by remember { mutableStateOf("Unknown Artist") }
    var currentCover by remember { mutableStateOf<String?>(null) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(audioUri) {
        if (audioUri == null) return@LaunchedEffect
        if (audioUri!!.scheme?.startsWith("http") == true) return@LaunchedEffect

        isFetching = true
        fetchError = null
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
                navController.navigate("player") { popUpTo("home") }
                return@LaunchedEffect
            }

            val rawFileName = getFileNameFromUri(context, audioUri!!)
            val cleanFileName = rawFileName.substringBeforeLast(".")
            var finalLrcResponse : LrcLibResponse?
            currentTitle = localMeta?.title ?: cleanFileName
            currentArtist = localMeta?.artist ?: "Unknown Artist"
            val findBestMatch = { results: List<LrcLibResponse> ->
                results
                    .filter { !it.syncedLyrics.isNullOrBlank() }
                    .take(8)
                    .minByOrNull { it.syncedLyrics!!.length }
            }
            try {
                finalLrcResponse = lrcLibApi.getLyrics(currentTitle, currentArtist)
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    var searchResults = lrcLibApi.searchLyrics("$currentTitle $currentArtist")
                    finalLrcResponse = findBestMatch(searchResults)

                    if (finalLrcResponse == null && cleanFileName.isNotBlank()) {
                        searchResults = lrcLibApi.searchLyrics(cleanFileName)
                        finalLrcResponse = findBestMatch(searchResults)
                    }
                } else {
                    throw e
                }
            }
            if (finalLrcResponse != null) {
                currentTitle = finalLrcResponse.trackName.ifBlank { cleanFileName }
                currentArtist = finalLrcResponse.artistName.ifBlank { "Unknown Artist" }
            } else {
                currentTitle = cleanFileName
            }
            val spotifyMeta = spotifyRepository.fetchCoverArtAndMeta("$currentTitle $currentArtist")
            currentCover = spotifyMeta?.coverArtUrl
            currentTitle = spotifyMeta?.title ?: currentTitle
            currentArtist = spotifyMeta?.artist ?: currentArtist
            val currentSpotifyId = spotifyMeta?.spotifyId
            val alreadyExists = cloudSongViewModel.checkSongExists(spotifyId = currentSpotifyId, title = currentTitle, artist = currentArtist)
            if (alreadyExists) {
                cloudSongViewModel.loadSongs()
                fetchError = "Song '$currentTitle' is already in your library!"
                isFetching = false
                return@LaunchedEffect
            }

            if (finalLrcResponse?.syncedLyrics != null ) {
                lyricsList = LrcParser.parse(finalLrcResponse.syncedLyrics)
                translatedLyrics = lyricTranslator.getFullSongTranslation(lyricsList)
                val audioBytes = context.contentResolver.openInputStream(audioUri!!)?.use { it.readBytes() }
                if (audioBytes != null) {
                    cloudSongViewModel.uploadSong(
                        spotifyId = currentSpotifyId, title = currentTitle, artist = currentArtist, audioBytes = audioBytes,
                        coverUrl = currentCover, syncedLyrics = lyricsList, translatedLyrics = translatedLyrics
                    )
                }
                isFetching = false
                navController.navigate("player") { popUpTo("home") }
            } else {
                fetchError = "No lyrics found online for $currentTitle"
            }
        } catch (e: Exception) {
            Log.e("TransLyricalFetch", "Pipeline critical failure", e)
            fetchError = "An error occurred while loading the song."
        } finally {
            isFetching = false
        }
    }

    RippleBackground(iconRes = R.drawable.ic_music_note) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        cloudViewModel = cloudSongViewModel,
                        onAudioSelected = { selectedUri ->
                            audioUri = null
                            audioUri = selectedUri
                            lyricsList = emptyList()
                            translatedLyrics = null
                            currentCover = null
                            fetchError = null
                        },
                        onCloudSongSelected = { cloudSong ->
                            audioUri = cloudSong.audioUrl.toUri()
                            lyricsList = cloudSong.syncedLyricsJson.toLyricsList()
                            translatedLyrics = cloudSong.translatedLyricsJson.toLyricsList()
                            currentTitle = cloudSong.title
                            currentArtist = cloudSong.artist
                            currentCover = cloudSong.coverUrl
                            isFetching = false
                            navController.navigate("player") { popUpTo("home") }
                        }
                    )
                    if (isFetching || fetchError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = .8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isFetching) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Fetching Synced Lyrics...", color = Color.White)
                                } else if (fetchError != null) {
                                    Text(
                                        text = fetchError!!,
                                        color = Color.White.copy(0.7f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Button(
                                        onClick = {
                                            fetchError = null
                                            audioUri = null
                                        }
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            composable("player") {
                val playerState = rememberLyricPlayer(lyricsList, audioUri)
                LyricScreen(playerState, translatedLyrics, currentTitle, currentArtist, currentCover)
            }
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
                1 -> LibraryScreen(
                    cloudViewModel,
                    onCloudSongSelected,
                    onDeleteSong = { cloudSong ->
                        cloudViewModel.deleteSong(cloudSong.id)
                    }
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: CloudSongViewModel,
    onCloudSongSelected: (CloudSong) -> Unit,
    onDeleteSong: (CloudSong) -> Unit
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

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadSongs() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.songs.isEmpty()) {
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        uiState.songs,
                        key = { song -> song.id }
                    ) { song ->
                        SongListItem(
                            song = song,
                            onClick = { onCloudSongSelected(song) },
                            onDeleteClick = { onDeleteSong(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongListItem(song: CloudSong, onClick: () -> Unit, onDeleteClick: () -> Unit) {
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

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete song",
                tint = MaterialTheme.colorScheme.error
            )
        }
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

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf("/")?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Unknown"
}
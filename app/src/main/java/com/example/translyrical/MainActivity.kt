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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.network.ITunesApi
import com.example.translyrical.network.ITunesTrack
import com.example.translyrical.network.LrcLibResponse
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(application)
                YoutubeDL.getInstance().updateYoutubeDL(application)
            } catch (e: Exception) {
                Log.e("YTDL_TEST", "Failed to initialize or update youtubedl-android", e)
            }
        }
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
    val coroutineScope = rememberCoroutineScope()

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var streamHeaders by remember { mutableStateOf<Map<String, String>?>(null) }
    var lyricsList by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var translatedLyrics by remember { mutableStateOf<List<LyricLine>?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Unknown Track") }
    var currentArtist by remember { mutableStateOf("Unknown Artist") }
    var currentCover by remember { mutableStateOf<String?>(null) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf("") }
    var editableArtist by remember { mutableStateOf("") }

    suspend fun runFetchingPipeline(
        searchTitle: String,
        searchArtist: String,
        fallbackFileName: String = "",
        isLocalFile: Boolean = false
    ) {
        isFetching = true
        fetchError = null
        showOverrideDialog = false

        try {
            var streamUrl: String? = null
            var ytDuration = 0
            var ytId: String? = null

            if (!isLocalFile) {
                val streamData = extractAudio("$searchTitle $searchArtist")

                if (streamData == null) {
                    fetchError = "Could not find audio stream on YouTube."
                    isFetching = false
                    return
                }
                streamUrl = streamData.url
                streamHeaders = streamData.headers
                ytDuration = streamData.durationSeconds
                ytId = streamData.youtubeId
                currentTitle = searchTitle
                currentArtist = searchArtist
            } else {
                currentTitle = searchTitle
                currentArtist = searchArtist
            }

            var finalLrcResponse: LrcLibResponse? = null
            try {
                var searchResults = lrcLibApi.searchLyrics("$searchTitle $searchArtist")
                if (searchResults.isEmpty() && fallbackFileName.isNotBlank()) {
                    searchResults = lrcLibApi.searchLyrics(fallbackFileName)
                }
                val validResults = searchResults.filter { !it.syncedLyrics.isNullOrBlank() }

                finalLrcResponse = if (!isLocalFile && ytDuration > 0) {
                    validResults.minByOrNull { abs((it.duration ?: 0.0) - ytDuration) }
                } else {
                    validResults.maxByOrNull { it.syncedLyrics!!.length }
                }

                if (finalLrcResponse == null) {
                    finalLrcResponse = lrcLibApi.getLyrics(searchTitle, searchArtist)
                }
            } catch (e: Exception) {
                Log.e("TransLyricalFetch", "LrcLib fetch failed", e)
            }

            if (finalLrcResponse?.syncedLyrics == null) {
                isFetching = false
                editableTitle = searchTitle
                editableArtist = searchArtist
                showOverrideDialog = true
                return
            }

            currentTitle = finalLrcResponse.trackName.ifBlank { currentTitle }
            currentArtist = finalLrcResponse.artistName.ifBlank { currentArtist }

            val spotifyMeta = spotifyRepository.fetchCoverArtAndMeta("$currentTitle $currentArtist")
            currentCover = spotifyMeta?.coverArtUrl ?: currentCover

            lyricsList = LrcParser.parse(finalLrcResponse.syncedLyrics)
            translatedLyrics = lyricTranslator.getFullSongTranslation(lyricsList)

            if (!isLocalFile) {
                audioUri = streamUrl?.toUri()
            } else {
                streamHeaders = null
            }

            cloudSongViewModel.uploadSong(
                ytId,
                currentTitle,
                currentArtist,
                currentCover,
                lyricsList,
                translatedLyrics
            )

            isFetching = false
            navController.navigate("player") { popUpTo("home") }

        } catch (e: Exception) {
            Log.e("TransLyricalFetch", "Pipeline critical failure", e)
            fetchError = "An error occurred while loading the song."
            isFetching = false
        }
    }

    LaunchedEffect(audioUri) {
        if (audioUri == null) return@LaunchedEffect

        if (audioUri!!.scheme?.startsWith("http") == true) return@LaunchedEffect

        val localMeta = extractMetadata(context, audioUri!!)
        val tempTitle = localMeta?.title ?: audioUri!!.lastPathSegment ?: "Unknown Track"
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

        val searchTitle = localMeta?.title ?: cleanFileName
        val searchArtist = localMeta?.artist ?: "Unknown Artist"

        runFetchingPipeline(searchTitle, searchArtist, cleanFileName, isLocalFile = true)
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Box(modifier = Modifier.fillMaxSize()) {
                MainScreen(
                    cloudViewModel = cloudSongViewModel,
                    onSearchRequested = { title, artist ->
                        coroutineScope.launch {
                            runFetchingPipeline(
                                title,
                                artist,
                                fallbackFileName = title,
                                isLocalFile = false
                            )
                        }
                    },
                    onAudioSelected = { selectedUri ->
                        audioUri = null
                        audioUri = selectedUri
                        lyricsList = emptyList()
                        translatedLyrics = null
                        currentCover = null
                        fetchError = null
                    },
                    onCloudSongSelected = { cloudSong ->
                        coroutineScope.launch {
                            isFetching = true

                            val streamData = if (!cloudSong.youtubeId.isNullOrBlank()) {
                                extractAudio(cloudSong.youtubeId, isDirectId = true)
                            } else {
                                extractAudio("${cloudSong.title} ${cloudSong.artist}", isDirectId = false)
                            }

                            if (streamData != null) {
                                audioUri = streamData.url.toUri()
                                streamHeaders = streamData.headers

                                lyricsList = cloudSong.syncedLyricsJson.toLyricsList()
                                translatedLyrics = cloudSong.translatedLyricsJson.toLyricsList()
                                currentTitle = cloudSong.title
                                currentArtist = cloudSong.artist
                                currentCover = cloudSong.coverUrl

                                isFetching = false
                                navController.navigate("player") { popUpTo("home") }
                            } else {
                                fetchError = "Could not connect to YouTube stream."
                                isFetching = false
                            }
                        }
                    },
                    onArtistTrackSelected = { title, artist ->
                        coroutineScope.launch {
                            runFetchingPipeline(title, artist, fallbackFileName = title, isLocalFile = false)
                        }
                    }
                )

                if (showOverrideDialog) {
                    MetadataOverrideDialog(
                        initialTitle = editableTitle,
                        initialArtist = editableArtist,
                        onDismiss = {
                            showOverrideDialog = false
                            audioUri = null
                        },
                        onRetry = { newTitle, newArtist ->
                            coroutineScope.launch {
                                runFetchingPipeline(newTitle, newArtist, newTitle, isLocalFile = (audioUri?.scheme != "http" && audioUri?.scheme != "https"))
                            }
                        }
                    )
                }
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
            val playerState = rememberLyricPlayer(lyricsList, audioUri, streamHeaders)
            LyricScreen(
                playerState,
                translatedLyrics,
                currentTitle,
                currentArtist,
                currentCover,
                audioUri,
                streamHeaders
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    cloudViewModel: CloudSongViewModel,
    onSearchRequested: (String, String) -> Unit,
    onAudioSelected: (Uri) -> Unit,
    onCloudSongSelected: (CloudSong) -> Unit,
    onArtistTrackSelected: (String, String) -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onAudioSelected(uri)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        floatingActionButton = {
            if (currentTab == 1) {
                FloatingActionButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add MP3")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Stream") },
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Artists") },
                    label = { Text("Artists") },
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when (currentTab) {
                0 -> HomeScreen(onSearchRequested)
                1 -> LibraryScreen(
                    cloudViewModel,
                    onCloudSongSelected,
                    onDeleteSong = { cloudSong ->
                        cloudViewModel.deleteSong(cloudSong.id)
                    }
                )
                2 -> ArtistScreen(onArtistTrackSelected)
            }
        }
    }
}

@Composable
fun HomeScreen(onSearchRequested: (String, String) -> Unit) {
    var showSearchDialog by remember { mutableStateOf(false) }

    RippleBackground(iconRes = 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { showSearchDialog = true },
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search & Stream",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        if (showSearchDialog) {
            StreamSearchDialog(
                onDismiss = { showSearchDialog = false },
                onSearch = { title, artist ->
                    showSearchDialog = false
                    onSearchRequested(title, artist)
                }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataOverrideDialog(
    initialTitle: String,
    initialArtist: String,
    onDismiss: () -> Unit,
    onRetry: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Lyrics Not Found",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "We couldn't find lyrics for this track. Edit the details below and try again.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onRetry(title.trim(), artist.trim()) },
                        enabled = title.isNotBlank() && artist.isNotBlank()
                    ) {
                        Text("Search Again")
                    }
                }
            }
        }
    }
}

data class StreamData(
    val url: String,
    val headers: Map<String, String>,
    val coverUrl: String?,
    val durationSeconds: Int,
    val youtubeId: String?
)

suspend fun extractAudio(searchQuery: String, isDirectId: Boolean = false): StreamData? {
    return withContext(Dispatchers.IO) {
        try {
            val requestString = if (isDirectId) {
                "ytsearch1:$searchQuery"
            } else {
                "ytsearch1:$searchQuery official audio"
            }

            val request = YoutubeDLRequest(requestString)

            request.addOption("-f", "bestaudio[ext=m4a]/bestaudio")
            request.addOption("--force-ipv4")
            request.addOption("--no-cache-dir")

            val info = YoutubeDL.getInstance().getInfo(request)
            if (info.url != null) {
                StreamData(
                    info.url!!,
                    info.httpHeaders ?: emptyMap(),
                    info.thumbnail,
                    info.duration,
                    info.id
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("YTDL", "Extraction failed", e)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Stream a Song",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onSearch(title.trim(), artist.trim()) },
                        enabled = title.isNotBlank() && artist.isNotBlank()
                    ) {
                        Text("Search")
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistScreen(
    onTrackSelected: (String, String) -> Unit,
    iTunesApi: ITunesApi = koinInject()
) {
    var searchQuery by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<ITunesTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val placeholderArtists = listOf(
        "Arijit Singh", "The Weeknd", "Karan Aujla",
        "Mazzy Star", "Kanye West", "Radiohead"
    )

    fun searchArtist(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch {
            isLoading = true
            try {
                val response = withContext(Dispatchers.IO) {
                    iTunesApi.getArtistTracks(query)
                }
                tracks = response.results.distinctBy { it.trackName }
            } catch (e: Exception) {
                Log.e("ITunes", "Failed to fetch tracks", e)
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search any artist...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { searchArtist(searchQuery) }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        )
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (tracks.isEmpty()) {
            Text(
                text = "Trending",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(placeholderArtists) { artist ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                searchQuery = artist
                                searchArtist(artist)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(artist, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(tracks) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onTrackSelected(track.trackName, track.artistName) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUrl100,
                            contentDescription = "Cover",
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(track.trackName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(track.artistName, color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
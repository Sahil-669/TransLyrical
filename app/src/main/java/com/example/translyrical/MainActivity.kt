package com.example.translyrical

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.translyrical.data.local.RecentSong
import com.example.translyrical.data.local.RecentSongDao
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.data.repository.SpotifyTrack
import com.example.translyrical.network.LrcLibResponse
import com.example.translyrical.ui.cleanTitle
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    val recentSongDao = koinInject<RecentSongDao>()
    val coroutineScope = rememberCoroutineScope()

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var streamHeaders by remember { mutableStateOf<Map<String, String>?>(null) }
    var lyricsList by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var translatedEnglish by remember { mutableStateOf<List<LyricLine>?>(null) }
    var translatedHindi by remember { mutableStateOf<List<LyricLine>?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Unknown Track") }
    var currentArtist by remember { mutableStateOf("Unknown Artist") }
    var currentCover by remember { mutableStateOf<String?>(null) }
    var currentYtId by remember { mutableStateOf<String?>(null) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf("") }
    var editableArtist by remember { mutableStateOf("") }
    var currentTranslationMode by remember { mutableIntStateOf(0) }

    val playerState = rememberLyricPlayer(lyricsList, audioUri, streamHeaders)

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
            var ytId: String?
            val streamDeferred = coroutineScope.async(Dispatchers.IO) {
                if (!isLocalFile) extractAudio("$searchTitle $searchArtist") else null
            }
            val spotifyDeferred = coroutineScope.async(Dispatchers.IO) {
                spotifyRepository.fetchCoverArtAndMeta("$searchTitle $searchArtist")
            }
            val streamData = streamDeferred.await()

            if (!isLocalFile) {
                if (streamData == null) {
                    fetchError = "Could not find audio stream on YouTube."
                    isFetching = false
                    return
                }
                streamUrl = streamData.url
                streamHeaders = streamData.headers
                ytDuration = streamData.durationSeconds
                ytId = streamData.youtubeId
                currentYtId = ytId
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

            val spotifyMeta = spotifyDeferred.await()
            currentTitle = finalLrcResponse.trackName.ifBlank { currentTitle }
            currentArtist = finalLrcResponse.artistName.ifBlank { currentArtist }
            currentCover = spotifyMeta?.coverArtUrl ?: currentCover

            lyricsList = LrcParser.parse(finalLrcResponse.syncedLyrics)
            translatedEnglish = null
            translatedHindi = null

            coroutineScope.launch(Dispatchers.IO) {
                val translation = lyricTranslator.getMultiLangTranslation(lyricsList)
                withContext(Dispatchers.Main) {
                    if (translation != null) {
                        translatedEnglish = translation.english
                        translatedHindi = translation.hindi
                        Toast.makeText(context, "Translations Loaded", Toast.LENGTH_SHORT).show()
                    } else {
                        translatedEnglish = emptyList()
                        translatedHindi = emptyList()
                        Toast.makeText(context, "AI is currently busy. Please try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            if (!isLocalFile) {
                audioUri = streamUrl?.toUri()
            } else {
                streamHeaders = null
            }

            coroutineScope.launch(Dispatchers.IO) {
                recentSongDao.insertOrUpdate(
                    RecentSong(
                        uniqueId = "${currentTitle.trim().lowercase()}-${currentArtist.trim().lowercase()}",
                        title = currentTitle,
                        artist = currentArtist,
                        coverUrl = currentCover,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

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
            translatedEnglish = existingSong.translatedEnglishJson.toLyricsList()
            translatedHindi = existingSong.translatedHindiJson.toLyricsList()
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
                        translatedEnglish = null
                        translatedHindi = null
                        currentCover = null
                        fetchError = null
                    },
                    onCloudSongSelected = { cloudSong ->
                        val isAlreadyPlaying = (cloudSong.youtubeId != null && cloudSong.youtubeId == currentYtId) ||
                                (cloudSong.title == currentTitle && cloudSong.artist == currentArtist)
                        if (isAlreadyPlaying && audioUri != null) {
                            navController.navigate("player") { popUpTo("home") }
                        } else {
                            coroutineScope.launch {
                                isFetching = true

                                val streamData = if (!cloudSong.youtubeId.isNullOrBlank()) {
                                    extractAudio(cloudSong.youtubeId, isDirectId = true)
                                } else {
                                    extractAudio(
                                        "${cloudSong.title} ${cloudSong.artist}",
                                        isDirectId = false
                                    )
                                }

                                if (streamData != null) {
                                    audioUri = streamData.url.toUri()
                                    streamHeaders = streamData.headers

                                    lyricsList = cloudSong.syncedLyricsJson.toLyricsList()
                                    translatedEnglish = cloudSong.translatedEnglishJson.toLyricsList()
                                    translatedHindi = cloudSong.translatedHindiJson.toLyricsList()
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
                        }
                    },
                    onArtistTrackSelected = { title, artist ->
                        val isAlreadyPlaying = (title == currentTitle && artist == currentArtist)
                        if (isAlreadyPlaying && audioUri != null) {
                            navController.navigate("player") { popUpTo("home") }
                        } else {
                            coroutineScope.launch {
                                runFetchingPipeline(
                                    title,
                                    artist,
                                    fallbackFileName = title,
                                    isLocalFile = false
                                )
                            }
                        }
                    },
                    showMiniPlayer = audioUri != null,
                    currentTitle,
                    currentArtist,
                    currentCover,
                    playerState.isPlaying,
                    onMiniPlayerClick = { navController.navigate("player") { popUpTo("home") } },
                    onPlayPauseClick = { playerState.togglePlayPause() }
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
            val isSaved = uiState.songs.any {
                it.youtubeId == currentYtId || (it.title == currentTitle && it.artist == currentArtist)
            }
            LyricScreen(
                playerState,
                translatedEnglish,
                translatedHindi,
                currentTitle,
                currentArtist,
                currentCover,
                audioUri,
                streamHeaders,
                isSaved,
                translationMode = currentTranslationMode,
                onTranslationModeChange = { currentTranslationMode = it },
                onSaveClick = {
                    if (!isSaved) {
                        cloudSongViewModel.uploadSong(
                            currentYtId, currentTitle, currentArtist,currentCover, lyricsList, translatedEnglish, translatedHindi
                        )
                        Toast.makeText(context, "Added to library!", Toast.LENGTH_SHORT).show()
                    } else {
                        val songToDelete = uiState.songs.find { it.youtubeId == currentYtId || (it.title == currentTitle && it.artist == currentArtist) }
                        songToDelete?.let { cloudSongViewModel.deleteSong(it.id) }
                        Toast.makeText(context, "Removed from library!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    cloudViewModel: CloudSongViewModel,
    onSearchRequested: (String, String) -> Unit,
    onAudioSelected: (Uri) -> Unit,
    onCloudSongSelected: (CloudSong) -> Unit,
    onArtistTrackSelected: (String, String) -> Unit,
    showMiniPlayer: Boolean,
    currentTitle: String,
    currentArtist: String,
    currentCover: String?,
    isPlaying: Boolean,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var showPreciseSearchDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onAudioSelected(uri)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF121212),
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "TransLyrical",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 32.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Discover") },
                    selected = currentTab == 0,
                    onClick = {
                        currentTab = 0
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.LightGray,
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.LightGray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                    label = { Text("Your Library") },
                    selected = currentTab == 1,
                    onClick = {
                        currentTab = 1
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.LightGray,
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.LightGray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Precise Search") },
                    selected = false,
                    onClick = {
                        showPreciseSearchDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = Color.LightGray,
                        unselectedIconColor = Color.LightGray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                    label = { Text("Load Local MP3") },
                    selected = false,
                    onClick = {
                        audioPickerLauncher.launch("audio/*")
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = Color.LightGray,
                        unselectedIconColor = Color.LightGray
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentTab == 0) "Discover" else "Library",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (currentTab == 0) Color(0xFF2B2B2B) else Color.Black                    )
                )
            },
            bottomBar = {
                if (showMiniPlayer) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onMiniPlayerClick() },
                        color = Color(0xFF2A2A2A)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentCover != null) {
                                AsyncImage(
                                    model = currentCover,
                                    contentDescription = "Cover",
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTitle.cleanTitle(),
                                    color = Color.White,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = currentArtist,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                            IconButton(onClick = onPlayPauseClick) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    0 -> ArtistScreen(onArtistTrackSelected)
                    1 -> LibraryScreen(
                        viewModel = cloudViewModel,
                        onCloudSongSelected = onCloudSongSelected,
                        onDeleteSong = { cloudSong -> cloudViewModel.deleteSong(cloudSong.id) }
                    )
                }
            }
            if (showPreciseSearchDialog) {
                StreamSearchDialog(
                    onDismiss = { showPreciseSearchDialog = false },
                    onSearch = { title, artist ->
                        showPreciseSearchDialog = false
                        onSearchRequested(title, artist)
                    }
                )
            }
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
                text = song.title.cleanTitle(),
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

data class PlaceholderArtist(val name: String, val imageUrl: String)

@Composable
fun ArtistScreen(
    onTrackSelected: (String, String) -> Unit,
    spotifyRepo: SpotifyRepository = koinInject(),
    recentSongDao: RecentSongDao = koinInject()
) {
    var searchQuery by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<SpotifyTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val recentSongs by recentSongDao.getRecentSongs().collectAsState(initial = emptyList())
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = tracks.isNotEmpty()) {
        tracks = emptyList()
        searchQuery = ""
    }

    val placeholderArtists = listOf(
        PlaceholderArtist("Arijit Singh", "https://i.scdn.co/image/ab6761610000e5ebadfb0b2df04b77e43b5f7375"),
        PlaceholderArtist("The Weeknd", "https://i.scdn.co/image/ab6761610000e5ebc1719ac9e6a75c1c25835018"),
        PlaceholderArtist("Karan Aujla", "https://i.scdn.co/image/ab6761610000e5eb5bc8823f5e12458215d64678"),
        PlaceholderArtist("Mazzy Star", "https://i.scdn.co/image/ab6772690000c46c93b4c6192035c98af64d4da3"),
        PlaceholderArtist("Kanye West", "https://i.scdn.co/image/ab6761610000e5eb6e835a500e791bf9c27a422a"),
        PlaceholderArtist("Radiohead", "https://i.scdn.co/image/ab6761610000e5eb959527d2fabc9c64287e57b9"),
        PlaceholderArtist("Taylor Swift", "https://i.scdn.co/image/ab6761610000e5eb12184bdd29403de54cb9d9c7"),
        PlaceholderArtist("Kendrick Lamar", "https://i.scdn.co/image/ab6761610000e5eb39ba6dcd4355c03de0b50918"),
        PlaceholderArtist("Diljit Dosanjh", "https://i.scdn.co/image/ab6761610000e5ebfc043bea91ac91c222d235c9")
    )

    fun searchArtist(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch {
            isLoading = true
            try {
                val results = withContext(Dispatchers.IO) {
                    spotifyRepo.searchTracks(query)
                }
                tracks = results.distinctBy { it.trackName }
            } catch (e: Exception) {
                Log.e("SpotifySearch", "Failed to fetch tracks", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B2B2B),
                        Color.Black
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("What do you want to listen to...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    searchArtist(searchQuery)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (tracks.isEmpty()) {
                Text(
                    text = "Artists",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(placeholderArtists) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = artist.name
                                    searchArtist(artist.name)
                                }
                        ) {
                            AsyncImage(
                                model = artist.imageUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)

                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = artist.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (recentSongs.isNotEmpty()) {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recentSongs) { song ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable {
                                        onTrackSelected(song.title, song.artist)
                                    }
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = song.artist,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Top Results",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.trackName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.artistName,
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Play",
                                tint = Color.White.copy(alpha = .5f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
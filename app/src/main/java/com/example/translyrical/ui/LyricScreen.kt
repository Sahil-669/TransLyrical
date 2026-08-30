package com.example.translyrical.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.translyrical.parser.LyricLine
import com.example.translyrical.player.LyricPlayerState
import com.example.translyrical.utils.downloadSongToDevice
import java.util.Locale
import kotlin.math.max

@Composable
fun LyricScreen(
    playerState: LyricPlayerState,
    translatedLyrics: List<LyricLine>?,
    songTitle: String,
    artistName: String,
    coverArt: String?,
    audioUri: Uri?,
    streamHeaders: Map<String, String>?,
    isSaved: Boolean,
    onSaveClick: () -> Unit
) {
    val context = LocalContext.current
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val heartInteractionSource = remember { MutableInteractionSource() }
    val heartIsPressed by heartInteractionSource.collectIsPressedAsState()
    val heartScale by animateFloatAsState(
        targetValue = if (heartIsPressed) .7f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )
    val playInteractionSource = remember { MutableInteractionSource() }
    val playIsPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (playIsPressed) .85f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "playScale"
    )

    LaunchedEffect(playerState.currentPositionMs) {
        if (!isDragging) {
            sliderPosition = playerState.currentPositionMs.toFloat()
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(playerState.activeLyricIndex) {
        val activeIndex = playerState.activeLyricIndex
        if (activeIndex >= 0) {
            listState.animateScrollToItem(max(0, activeIndex - 3))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        if (coverArt != null) {
            AsyncImage(
                model = coverArt,
                contentDescription = "Song Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.3f }

            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = songTitle,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = artistName,
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = {
                        downloadSongToDevice(
                            context,
                            audioUri,
                            songTitle,
                            artistName,
                            streamHeaders
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Song",
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 150.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(playerState.lyricsList) { index, lyric ->
                    val isActive = (index == playerState.activeLyricIndex)
                    val englishText = translatedLyrics?.getOrNull(index)?.text
                    LyricRow(
                        lyric = lyric,
                        translatedText = englishText,
                        isActive = isActive,
                        isTranslationToggledOn = isActive
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        isDragging = true
                        sliderPosition = newValue
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        playerState.seekTo(sliderPosition.toLong())
                    },
                    valueRange = 0f..(playerState.durationMs.coerceAtLeast(1L).toFloat()),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTickColor = Color.White,
                        inactiveTickColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimestamp(sliderPosition.toLong()),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTimestamp(playerState.durationMs),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSaveClick,
                        interactionSource = heartInteractionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            }
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save to Library",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = playScale
                                scaleY = playScale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = LocalIndication.current
                            ) { playerState.togglePlayPause() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate Lyrics",
                            tint = Color.White
                        )
                    }
                }

            }
        }
    }

}

@Composable
fun LyricRow(
    lyric: LyricLine,
    translatedText: String?,
    isActive: Boolean,
    isTranslationToggledOn: Boolean
) {
    val targetAlpha = if (isActive) 1f else 0.4f
    val targetScale = if (isActive) 1.1f else 0.9f

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(300),
        label = "alpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(300),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
    ) {
        Text(
            text = lyric.text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        AnimatedVisibility(
            visible = isTranslationToggledOn,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (translatedText != null) {
                Text(
                    text = translatedText,
                    color = Color(0xFFB3B3B3),
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

fun formatTimestamp(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US,"%02d:%02d", minutes, seconds)
}


package com.example.translyrical.player

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.translyrical.parser.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberLyricPlayer(
    lyricsList: List<LyricLine>,
    audioUri: Uri?
): LyricPlayerState {
    val context = LocalContext.current

    val exoPlayer = retain {
        ExoPlayer.Builder(context).build()
    }

    val playerState = remember(exoPlayer) {
        LyricPlayerState(exoPlayer)
    }

    LaunchedEffect(lyricsList) {
        playerState.lyricsList = lyricsList
    }

    LaunchedEffect(audioUri) {
        if (audioUri == null) return@LaunchedEffect
        playerState.isBuffering = true
        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(audioUri))
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            playerState.isBuffering = false
        }
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            playerState.currentPositionMs = exoPlayer.currentPosition
            playerState.isPlaying = exoPlayer.isPlaying

            val rawDuration = exoPlayer.duration
            playerState.durationMs = if (rawDuration > 0) rawDuration else 0L

            delay(50.milliseconds)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    return playerState
}

class LyricPlayerState (
    val player: ExoPlayer,
) {
    var currentPositionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var lyricsList by mutableStateOf<List<LyricLine>>(emptyList())
    val activeLyricIndex: Int
        get() {
            if (lyricsList.isEmpty()) return -1

            return lyricsList.indexOfLast { lyricLine ->
                lyricLine.startTimeMs <= currentPositionMs
            }
        }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        currentPositionMs = positionMs
    }
}
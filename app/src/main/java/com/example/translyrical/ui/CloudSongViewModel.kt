package com.example.translyrical.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.translyrical.data.repository.CloudSongRepository
import com.example.translyrical.domain.CloudSong
import com.example.translyrical.parser.LyricLine
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CloudSongUiState(
    val isLoading: Boolean = false,
    val songs: List<CloudSong> = emptyList(),
    val error: String? = null
)

class CloudSongViewModel(
    private val repository: CloudSongRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(CloudSongUiState())
    val uiState: StateFlow<CloudSongUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getCloudSongs().fold(
                onSuccess = { songsList ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        songs = songsList
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "An unknown error occurred."
                    )
                }
            )
        }
    }

    suspend fun checkSongExists(spotifyId: String?, title: String, artist: String): Boolean {
        return repository.getExistingSong(spotifyId, title, artist) != null
    }

    fun uploadSong(
        spotifyId: String?,
        title: String,
        artist: String,
        audioBytes: ByteArray,
        coverUrl: String?,
        syncedLyrics: List<LyricLine>,
        translatedLyrics: List<LyricLine>?
        ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val existingSong = repository.getExistingSong(spotifyId, title, artist)
            if (existingSong != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Song is already in your library."
                )
                return@launch
            }
            val syncedJson = gson.toJson(syncedLyrics)
            val translatedJson = translatedLyrics?.let { gson.toJson(it) }

            repository.uploadCloudSong(
                spotifyId,
                title,
                artist,
                audioBytes,
                coverUrl,
                syncedJson,
                translatedJson
            ).fold(
                onSuccess = {
                    loadSongs()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "An unknown error occurred."
                    )
                }
            )
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            try {


                repository.deleteSong(songId)
                val updatedSongs = _uiState.value.songs.filter { it.id != songId }
                _uiState.value = _uiState.value.copy(
                    songs = updatedSongs
                )
            } catch (_ : Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete song from cloud."
                )
            }
        }
    }
}
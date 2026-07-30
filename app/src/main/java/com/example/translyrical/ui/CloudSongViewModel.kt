package com.example.translyrical.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.translyrical.data.repository.CloudSongRepository
import com.example.translyrical.domain.CloudSong
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

    fun uploadSong(title: String, artist: String, audioBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.uploadCloudSong(title, artist, audioBytes).fold(
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
}
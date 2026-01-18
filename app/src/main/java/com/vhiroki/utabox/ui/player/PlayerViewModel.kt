package com.vhiroki.utabox.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vhiroki.utabox.data.Song
import com.vhiroki.utabox.util.VideoStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val song: Song? = null,
    val videoUri: Uri? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class PlayerViewModel(
    private val videoStorageHelper: VideoStorageHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadVideo(song: Song) {
        _uiState.value = PlayerUiState(song = song, isLoading = true)

        val videoUri = videoStorageHelper.getVideoUri(song.musicId)

        if (videoUri != null) {
            _uiState.value = PlayerUiState(
                song = song,
                videoUri = videoUri,
                isLoading = false
            )
        } else {
            _uiState.value = PlayerUiState(
                song = song,
                isLoading = false,
                error = "Video file not found: ${song.musicId}.mp4"
            )
        }
    }

    class Factory(
        private val videoStorageHelper: VideoStorageHelper
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PlayerViewModel(videoStorageHelper) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

package com.vhiroki.utabox.data

import com.vhiroki.utabox.util.VideoStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class SongRepository(
    private val csvSongReader: CsvSongReader,
    private val videoStorageHelper: VideoStorageHelper
) {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    val error: Flow<String?> = _error

    fun getAllSongs(): Flow<List<Song>> = _songs

    fun searchSongs(query: String): Flow<List<Song>> {
        return if (query.isBlank()) {
            _songs
        } else {
            val trimmedQuery = query.trim().lowercase()
            _songs.map { songs ->
                songs.filter { song ->
                    song.code.lowercase().startsWith(trimmedQuery) ||
                    song.title.lowercase().contains(trimmedQuery) ||
                    song.artist.lowercase().contains(trimmedQuery)
                }.sortedWith(
                    compareBy(
                        { !it.code.lowercase().startsWith(trimmedQuery) },
                        { it.artist.lowercase() },
                        { it.title.lowercase() }
                    )
                )
            }
        }
    }

    /**
     * Load or reload songs from CSV files in the video source directory.
     * Priority: 1. User-selected folder (SAF), 2. Test folder/USB (direct file access)
     * Returns error message if loading fails, null on success.
     */
    fun reload(): String? {
        _error.value = null

        // Try persisted folder first (SAF - has full access to all file types)
        val result = videoStorageHelper.getPersistedTreeUri()?.let { uri ->
            csvSongReader.readFromDocumentFolder(uri)
        } ?: videoStorageHelper.getVideoSourceDirectory()?.let { directory ->
            // Fall back to direct file access (limited on Android 11+)
            csvSongReader.readFromDirectory(directory)
        } ?: Result.failure(Exception("No video source configured"))

        return result.fold(
            onSuccess = { songs ->
                _songs.value = songs.sortedWith(
                    compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
                )
                null
            },
            onFailure = { exception ->
                // Clear songs list so we don't show stale data from previous folder
                _songs.value = emptyList()
                val errorMsg = exception.message ?: "Failed to load songs"
                _error.value = errorMsg
                errorMsg
            }
        )
    }
}

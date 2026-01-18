package com.vhiroki.utabox.data

import com.vhiroki.utabox.util.VideoStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.Normalizer

class SongRepository(
    private val csvSongReader: CsvSongReader,
    private val videoStorageHelper: VideoStorageHelper,
    private val youTubeSongLoader: YouTubeSongLoader
) {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    // Store local and YouTube songs separately so we can merge them
    private var localSongs: List<Song> = emptyList()
    private var youtubeSongs: List<Song> = emptyList()

    val error: Flow<String?> = _error

    fun getAllSongs(): Flow<List<Song>> = _songs

    fun searchSongs(query: String): Flow<List<Song>> {
        return if (query.isBlank()) {
            _songs
        } else {
            val trimmedQuery = query.trim().lowercase().removeDiacritics()
            _songs.map { songs ->
                songs.filter { song ->
                    song.code.lowercase().startsWith(trimmedQuery) ||
                    song.title.lowercase().removeDiacritics().contains(trimmedQuery) ||
                    song.artist.lowercase().removeDiacritics().contains(trimmedQuery)
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
     * Removes diacritics (accents) from a string for accent-insensitive comparison.
     * For example: "pão" becomes "pao", "café" becomes "cafe"
     */
    private fun String.removeDiacritics(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    /**
     * Load or reload songs from both local CSV files and YouTube repository.
     * Returns error message if both sources fail, null on success.
     */
    suspend fun reload(): String? {
        _error.value = null

        var localError: String? = null
        var youtubeError: String? = null

        // Load local songs
        val localResult = videoStorageHelper.getPersistedTreeUri()?.let { uri ->
            csvSongReader.readFromDocumentFolder(uri)
        } ?: videoStorageHelper.getVideoSourceDirectory()?.let { directory ->
            csvSongReader.readFromDirectory(directory)
        }

        localResult?.fold(
            onSuccess = { songs -> localSongs = songs },
            onFailure = { e ->
                localSongs = emptyList()
                localError = e.message
            }
        ) ?: run {
            localSongs = emptyList()
            localError = "No video source configured"
        }

        // Load YouTube songs
        youTubeSongLoader.loadSongs().fold(
            onSuccess = { songs -> youtubeSongs = songs },
            onFailure = { e ->
                youtubeSongs = emptyList()
                youtubeError = e.message
            }
        )

        // Merge songs (local songs take priority if same code)
        val allSongs = (youtubeSongs + localSongs)
            .associateBy { it.code }
            .values
            .sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))

        _songs.value = allSongs

        // Only return error if both sources failed
        return if (allSongs.isEmpty()) {
            val errorMsg = listOfNotNull(localError, youtubeError).joinToString("; ")
            _error.value = errorMsg
            errorMsg
        } else {
            null
        }
    }
}

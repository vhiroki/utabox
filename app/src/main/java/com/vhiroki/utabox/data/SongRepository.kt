package com.vhiroki.utabox.data

import com.vhiroki.utabox.data.Song.Companion.removeDiacritics
import com.vhiroki.utabox.util.VideoStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

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

    // Maximum number of results to return to avoid memory issues
    companion object {
        const val MAX_SEARCH_RESULTS = 200
    }

    val error: Flow<String?> = _error

    fun getAllSongs(): Flow<List<Song>> = _songs

    fun searchSongs(query: String): Flow<List<Song>> {
        return if (query.isBlank()) {
            // When no query, return limited results to avoid memory issues
            _songs.map { songs -> songs.take(MAX_SEARCH_RESULTS) }
        } else {
            val trimmedQuery = query.trim().lowercase().removeDiacritics()
            _songs.map { songs ->
                // Use sequence for lazy evaluation - stop early when we have enough results
                val codeMatches = mutableListOf<Song>()
                val titleMatches = mutableListOf<Song>()
                val artistMatches = mutableListOf<Song>()

                for (song in songs) {
                    when {
                        // Code prefix match (highest priority)
                        song.codeLower.startsWith(trimmedQuery) -> codeMatches.add(song)
                        // Title contains match
                        song.titleNormalized.contains(trimmedQuery) -> titleMatches.add(song)
                        // Artist contains match
                        song.artistNormalized.contains(trimmedQuery) -> artistMatches.add(song)
                    }

                    // Early termination if we have enough results
                    if (codeMatches.size + titleMatches.size + artistMatches.size >= MAX_SEARCH_RESULTS) {
                        break
                    }
                }

                // Combine results with priority: code > title > artist
                // Sort each category and limit total results
                (codeMatches.sortedWith(compareBy({ it.artistNormalized }, { it.titleNormalized })) +
                 titleMatches.sortedWith(compareBy({ it.artistNormalized }, { it.titleNormalized })) +
                 artistMatches.sortedWith(compareBy({ it.artistNormalized }, { it.titleNormalized })))
                    .take(MAX_SEARCH_RESULTS)
            }
        }
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

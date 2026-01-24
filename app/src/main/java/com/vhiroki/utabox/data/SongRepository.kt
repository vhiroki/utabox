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
    private val _lookupReport = MutableStateFlow<LookupReport?>(null)

    // Store local and YouTube songs separately so we can merge them
    private var localSongs: List<Song> = emptyList()
    private var youtubeSongs: List<Song> = emptyList()

    // Maximum number of results to return to avoid memory issues
    companion object {
        const val MAX_SEARCH_RESULTS = 200
    }

    val error: Flow<String?> = _error
    val lookupReport: Flow<LookupReport?> = _lookupReport

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
     * Returns LookupReport with details about each source.
     */
    suspend fun reload(): LookupReport {
        _error.value = null

        var localSourceReport: SourceReport? = null
        var youtubeSourceReport: SourceReport? = null

        // Load local songs
        val localResult = videoStorageHelper.getPersistedTreeUri()?.let { uri ->
            csvSongReader.readFromDocumentFolder(uri)
        } ?: videoStorageHelper.getVideoSourceDirectory()?.let { directory ->
            csvSongReader.readFromDirectory(directory)
        }

        localResult?.fold(
            onSuccess = { result ->
                localSongs = result.songs
                localSourceReport = SourceReport(
                    sourceName = "USB/Local Storage",
                    folderPath = result.directoryPath,
                    csvFiles = result.csvFileReports,
                    songCount = result.songs.size
                )
            },
            onFailure = { e ->
                localSongs = emptyList()
                localSourceReport = SourceReport(
                    sourceName = "USB/Local Storage",
                    folderPath = videoStorageHelper.getVideoSourceDirectory()?.absolutePath,
                    csvFiles = emptyList(),
                    songCount = 0,
                    error = e.message
                )
            }
        ) ?: run {
            localSongs = emptyList()
            localSourceReport = SourceReport(
                sourceName = "USB/Local Storage",
                folderPath = null,
                csvFiles = emptyList(),
                songCount = 0,
                error = "No USB drive with 'videoke' folder detected"
            )
        }

        // Load YouTube songs
        youTubeSongLoader.loadSongs().fold(
            onSuccess = { result ->
                youtubeSongs = result.songs
                youtubeSourceReport = SourceReport(
                    sourceName = "YouTube",
                    folderPath = result.sourcePath,
                    csvFiles = result.csvFileReports,
                    songCount = result.songs.size
                )
            },
            onFailure = { e ->
                youtubeSongs = emptyList()
                youtubeSourceReport = SourceReport(
                    sourceName = "YouTube",
                    folderPath = null,
                    csvFiles = emptyList(),
                    songCount = 0,
                    error = e.message
                )
            }
        )

        // Merge songs (local songs take priority if same code)
        val allSongs = (youtubeSongs + localSongs)
            .associateBy { it.code }
            .values
            .sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))

        _songs.value = allSongs

        // Build lookup report
        val report = LookupReport(
            localReport = localSourceReport,
            youtubeReport = youtubeSourceReport
        )
        _lookupReport.value = report

        // Set error if both sources failed
        if (allSongs.isEmpty()) {
            val errorMsg = listOfNotNull(
                localSourceReport?.error,
                youtubeSourceReport?.error
            ).joinToString("; ")
            _error.value = errorMsg
        }

        return report
    }
}

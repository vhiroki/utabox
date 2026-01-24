package com.vhiroki.utabox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of loading YouTube songs, including detailed reports.
 */
data class YouTubeLoadResult(
    val songs: List<Song>,
    val csvFileReports: List<CsvFileReport>,
    val sourcePath: String
)

/**
 * Loads YouTube karaoke songs from CSV files hosted on GitHub.
 * Repository: https://github.com/vhiroki/utabox/tree/main/youtube-songs
 */
class YouTubeSongLoader {

    companion object {
        private const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/vhiroki/utabox/main/youtube-songs"
        private const val GITHUB_API_BASE = "https://api.github.com/repos/vhiroki/utabox/contents/youtube-songs"

        // Index file that lists all CSV files - avoids GitHub API rate limits
        private const val INDEX_FILE = "index.txt"
    }

    /**
     * Fetch all YouTube songs from all CSV files in the repository.
     * @return Result with YouTubeLoadResult including songs and file reports, or failure with error message
     */
    suspend fun loadSongs(): Result<YouTubeLoadResult> = withContext(Dispatchers.IO) {
        try {
            // Get list of CSV files - try index file first, then fallback to GitHub API
            val csvFiles = fetchCsvFileListFromIndex() ?: fetchCsvFileListFromApi()

            if (csvFiles.isEmpty()) {
                return@withContext Result.failure(Exception("No CSV files found"))
            }

            val songsMap = mutableMapOf<String, Song>()
            val fileReports = mutableListOf<CsvFileReport>()

            // Fetch and parse each CSV file
            for (filename in csvFiles) {
                try {
                    val beforeCount = songsMap.size
                    val csvContent = fetchFileContent(filename)
                    parseCsv(csvContent, songsMap)
                    val songsFromFile = songsMap.size - beforeCount
                    fileReports.add(CsvFileReport(filename, songsFromFile))
                } catch (e: Exception) {
                    // Log but continue with other files
                    android.util.Log.e("YouTubeSongLoader", "Failed to load $filename: ${e.message}")
                    fileReports.add(CsvFileReport(filename, 0, e.message))
                }
            }

            if (songsMap.isEmpty()) {
                Result.failure(Exception("No YouTube songs found"))
            } else {
                Result.success(YouTubeLoadResult(songsMap.values.toList(), fileReports, GITHUB_RAW_BASE))
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeSongLoader", "Error loading songs: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Try to fetch list of CSV files from index.txt file.
     * Returns null if index file doesn't exist or can't be read.
     */
    private fun fetchCsvFileListFromIndex(): List<String>? {
        return try {
            val url = URL("$GITHUB_RAW_BASE/$INDEX_FILE")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val files = content.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.endsWith(".csv") }
                if (files.isNotEmpty()) files else null
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch list of CSV files from GitHub API (fallback, may be rate limited).
     */
    private fun fetchCsvFileListFromApi(): List<String> {
        return try {
            val url = URL(GITHUB_API_BASE)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                // Parse JSON response to extract .csv filenames
                // Simple regex parsing to avoid adding JSON library dependency
                val pattern = """"name"\s*:\s*"([^"]+\.csv)"""".toRegex()
                pattern.findAll(response).map { it.groupValues[1] }.toList()
            } else {
                android.util.Log.e("YouTubeSongLoader", "GitHub API error: ${connection.responseCode}")
                connection.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeSongLoader", "GitHub API request failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetch raw content of a CSV file from GitHub.
     */
    private fun fetchFileContent(filename: String): String {
        val url = URL("$GITHUB_RAW_BASE/$filename")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                throw Exception("Failed to fetch $filename: HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse CSV content and add songs to the map.
     * CSV format: code,artist,title,url,notes
     * Duplicate codes are overwritten (last occurrence wins).
     */
    private fun parseCsv(content: String, songsMap: MutableMap<String, Song>) {
        val lines = content.lines()
        android.util.Log.d("YouTubeSongLoader", "Parsing CSV with ${lines.size} lines")

        var parsedCount = 0
        var skippedCount = 0

        for ((index, line) in lines.withIndex()) {
            // Skip header line
            if (index == 0) continue
            if (line.isBlank()) continue

            val song = parseCsvLine(line)
            if (song != null) {
                songsMap[song.code] = song
                parsedCount++
            } else {
                skippedCount++
                if (skippedCount <= 3) {
                    android.util.Log.d("YouTubeSongLoader", "Failed to parse line $index: ${line.take(100)}")
                }
            }
        }
        android.util.Log.d("YouTubeSongLoader", "Parsed $parsedCount songs, skipped $skippedCount lines")
    }

    /**
     * Parse a single CSV line into a Song.
     * Expected format: code,artist,title,url,notes
     * Handles quoted fields (e.g., titles with commas)
     */
    private fun parseCsvLine(line: String): Song? {
        val parts = parseCSVFields(line)
        if (parts.size < 4) return null

        val code = parts[0].trim()
        val artist = parts[1].trim()
        val title = parts[2].trim()
        val url = parts[3].trim()
        val notes = if (parts.size > 4) parts[4].trim().takeIf { it.isNotEmpty() } else null

        if (code.isBlank() || url.isBlank()) return null

        return Song(
            code = code,
            filename = "", // No local file for YouTube songs
            artist = artist,
            title = title,
            youtubeUrl = url,
            notes = notes
        )
    }

    /**
     * Parse CSV fields handling quoted values with commas.
     */
    private fun parseCSVFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())

        return fields
    }
}

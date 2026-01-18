package com.vhiroki.utabox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads YouTube karaoke songs from CSV files hosted on GitHub.
 * Repository: https://github.com/vhiroki/utabox/tree/main/youtube-songs
 */
class YouTubeSongLoader {

    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com/repos/vhiroki/utabox/contents/youtube-songs"
        private const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/vhiroki/utabox/main/youtube-songs"
    }

    /**
     * Fetch all YouTube songs from all CSV files in the repository.
     * @return Result with list of songs, or failure with error message
     */
    suspend fun loadSongs(): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            // Get list of CSV files from GitHub API
            val csvFiles = fetchCsvFileList()

            if (csvFiles.isEmpty()) {
                return@withContext Result.failure(Exception("No CSV files found in YouTube songs repository"))
            }

            val songsMap = mutableMapOf<String, Song>()

            // Fetch and parse each CSV file
            for (filename in csvFiles) {
                try {
                    val csvContent = fetchFileContent(filename)
                    parseCsv(csvContent, songsMap)
                } catch (e: Exception) {
                    // Log but continue with other files
                    android.util.Log.e("YouTubeSongLoader", "Failed to load $filename: ${e.message}")
                }
            }

            if (songsMap.isEmpty()) {
                Result.failure(Exception("No YouTube songs found"))
            } else {
                Result.success(songsMap.values.toList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch list of CSV files from GitHub repository.
     */
    private fun fetchCsvFileList(): List<String> {
        val url = URL(GITHUB_API_BASE)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                // Parse JSON response to extract .csv filenames
                // Simple regex parsing to avoid adding JSON library dependency
                val pattern = """"name"\s*:\s*"([^"]+\.csv)"""".toRegex()
                pattern.findAll(response).map { it.groupValues[1] }.toList()
            } else {
                android.util.Log.e("YouTubeSongLoader", "GitHub API error: ${connection.responseCode}")
                emptyList()
            }
        } finally {
            connection.disconnect()
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
     * CSV format: code,title,artist,url
     * Duplicate codes are overwritten (last occurrence wins).
     */
    private fun parseCsv(content: String, songsMap: MutableMap<String, Song>) {
        val lines = content.lines()

        for ((index, line) in lines.withIndex()) {
            // Skip header line
            if (index == 0) continue
            if (line.isBlank()) continue

            val song = parseCsvLine(line)
            if (song != null) {
                songsMap[song.code] = song
            }
        }
    }

    /**
     * Parse a single CSV line into a Song.
     * Expected format: code,title,artist,url
     * Handles quoted fields (e.g., titles with commas)
     */
    private fun parseCsvLine(line: String): Song? {
        val parts = parseCSVFields(line)
        if (parts.size < 4) return null

        val code = parts[0].trim()
        val title = parts[1].trim()
        val artist = parts[2].trim()
        val url = parts[3].trim()

        if (code.isBlank() || url.isBlank()) return null

        return Song(
            code = code,
            filename = "", // No local file for YouTube songs
            artist = artist,
            title = title,
            youtubeUrl = url
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

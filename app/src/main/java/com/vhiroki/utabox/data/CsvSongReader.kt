package com.vhiroki.utabox.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Reads songs from CSV files in the video source directory.
 * CSV format: code,filename,artist,title,lyrics_preview
 * If duplicate codes exist across files, the last occurrence wins.
 */
class CsvSongReader(private val context: Context) {

    /**
     * Read all songs from CSV files in the given directory.
     * @param directory File directory (for test folder or USB)
     * @return List of songs, with duplicates resolved by last occurrence
     */
    fun readFromDirectory(directory: File): Result<List<Song>> {
        return try {
            val songsMap = mutableMapOf<String, Song>()

            val csvFiles = directory.listFiles { file ->
                file.extension.equals("csv", ignoreCase = true)
            }?.toList() ?: emptyList()

            for (csvFile in csvFiles) {
                csvFile.bufferedReader().use { reader ->
                    parseCsv(reader, songsMap)
                }
            }

            if (songsMap.isEmpty()) {
                Result.failure(Exception("No CSV files found in ${directory.absolutePath}"))
            } else {
                Result.success(songsMap.values.toList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read all songs from CSV files using DocumentFile (for SAF/persisted folders).
     * @param folderUri The URI of the folder
     * @return List of songs, with duplicates resolved by last occurrence
     */
    fun readFromDocumentFolder(folderUri: Uri): Result<List<Song>> {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
                ?: return Result.failure(Exception("Cannot access folder"))

            val csvFiles = documentFile.listFiles().filter {
                it.name?.endsWith(".csv", ignoreCase = true) == true
            }

            if (csvFiles.isEmpty()) {
                return Result.failure(Exception("No CSV files found in selected folder"))
            }

            val songsMap = mutableMapOf<String, Song>()

            for (csvDoc in csvFiles) {
                csvDoc.uri.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            parseCsv(reader, songsMap)
                        }
                    }
                }
            }

            if (songsMap.isEmpty()) {
                Result.failure(Exception("No songs found in CSV files"))
            } else {
                Result.success(songsMap.values.toList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse CSV content and add songs to the map.
     * Duplicate codes are overwritten (last occurrence wins).
     */
    private fun parseCsv(reader: BufferedReader, songsMap: MutableMap<String, Song>) {
        var isFirstLine = true

        reader.forEachLine { line ->
            if (isFirstLine) {
                isFirstLine = false
                // Skip header line
                return@forEachLine
            }

            val song = parseCsvLine(line)
            if (song != null) {
                songsMap[song.code] = song
            }
        }
    }

    /**
     * Parse a single CSV line into a Song.
     * Expected format: code,filename,artist,title,lyrics_preview
     */
    private fun parseCsvLine(line: String): Song? {
        if (line.isBlank()) return null

        val parts = line.split(",")
        if (parts.size < 4) return null

        val code = parts[0].trim()
        val filename = parts[1].trim()
        val artist = parts[2].trim()
        val title = parts[3].trim()
        // lyrics_preview (parts[4]) is intentionally ignored

        if (code.isBlank() || filename.isBlank()) return null

        return Song(
            code = code,
            filename = filename,
            artist = artist,
            title = title
        )
    }
}

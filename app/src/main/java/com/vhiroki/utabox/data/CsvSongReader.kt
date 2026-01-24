package com.vhiroki.utabox.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Result of reading songs from CSV files, including detailed reports.
 */
data class CsvReadResult(
    val songs: List<Song>,
    val csvFileReports: List<CsvFileReport>,
    val directoryPath: String
)

/**
 * Reads songs from CSV files in the video source directory.
 * CSV format: code,filename,artist,title,lyrics_preview
 * If duplicate codes exist across files, the last occurrence wins.
 */
class CsvSongReader(private val context: Context) {

    /**
     * Read all songs from CSV files in the given directory.
     * @param directory File directory (for test folder or USB)
     * @return CsvReadResult with songs and per-file reports
     */
    fun readFromDirectory(directory: File): Result<CsvReadResult> {
        return try {
            val songsMap = mutableMapOf<String, Song>()
            val fileReports = mutableListOf<CsvFileReport>()

            val csvFiles = directory.listFiles { file ->
                file.extension.equals("csv", ignoreCase = true)
            }?.toList() ?: emptyList()

            for (csvFile in csvFiles) {
                try {
                    val beforeCount = songsMap.size
                    csvFile.bufferedReader().use { reader ->
                        parseCsv(reader, songsMap)
                    }
                    val songsFromFile = songsMap.size - beforeCount
                    fileReports.add(CsvFileReport(csvFile.name, songsFromFile))
                } catch (e: Exception) {
                    fileReports.add(CsvFileReport(csvFile.name, 0, e.message))
                }
            }

            if (csvFiles.isEmpty()) {
                Result.failure(Exception("No CSV files found in ${directory.absolutePath}"))
            } else {
                Result.success(CsvReadResult(songsMap.values.toList(), fileReports, directory.absolutePath))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read all songs from CSV files using DocumentFile (for SAF/persisted folders).
     * Expects the folder to be or contain a "videoke" folder with CSV files.
     * @param folderUri The URI of the folder
     * @return CsvReadResult with songs and per-file reports
     */
    fun readFromDocumentFolder(folderUri: Uri): Result<CsvReadResult> {
        return try {
            android.util.Log.d("CsvSongReader", "Reading from folder: $folderUri")

            val documentFile = try {
                DocumentFile.fromTreeUri(context, folderUri)
            } catch (e: Exception) {
                android.util.Log.e("CsvSongReader", "Failed to create DocumentFile from URI", e)
                return Result.failure(Exception("Cannot access folder: ${e.message}"))
            }

            if (documentFile == null) {
                android.util.Log.e("CsvSongReader", "DocumentFile is null for URI: $folderUri")
                return Result.failure(Exception("Cannot access folder"))
            }

            android.util.Log.d("CsvSongReader", "DocumentFile created, listing files...")

            // First, try to find a "videoke" subfolder
            val targetFolder = documentFile.findFile("videoke") ?: documentFile
            val folderPath = targetFolder.uri.path ?: folderUri.toString()

            val allFiles = try {
                targetFolder.listFiles()
            } catch (e: Exception) {
                android.util.Log.e("CsvSongReader", "Failed to list files in folder", e)
                return Result.failure(Exception("Cannot list files in folder: ${e.message}"))
            }

            android.util.Log.d("CsvSongReader", "Found ${allFiles.size} files in folder")

            val csvFiles = allFiles.filter {
                it.name?.endsWith(".csv", ignoreCase = true) == true
            }

            android.util.Log.d("CsvSongReader", "Found ${csvFiles.size} CSV files")

            if (csvFiles.isEmpty()) {
                return Result.failure(Exception("No CSV files found in selected folder"))
            }

            val songsMap = mutableMapOf<String, Song>()
            val fileReports = mutableListOf<CsvFileReport>()

            for (csvDoc in csvFiles) {
                val filename = csvDoc.name ?: "unknown.csv"
                try {
                    android.util.Log.d("CsvSongReader", "Reading CSV: $filename")
                    val beforeCount = songsMap.size
                    csvDoc.uri.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                parseCsv(reader, songsMap)
                            }
                        }
                    }
                    val songsFromFile = songsMap.size - beforeCount
                    fileReports.add(CsvFileReport(filename, songsFromFile))
                } catch (e: Exception) {
                    android.util.Log.e("CsvSongReader", "Error reading CSV $filename: ${e.message}", e)
                    fileReports.add(CsvFileReport(filename, 0, e.message))
                }
            }

            android.util.Log.d("CsvSongReader", "Loaded ${songsMap.size} songs from CSV files")

            if (songsMap.isEmpty()) {
                Result.failure(Exception("No songs found in CSV files"))
            } else {
                Result.success(CsvReadResult(songsMap.values.toList(), fileReports, folderPath))
            }
        } catch (e: Exception) {
            android.util.Log.e("CsvSongReader", "Unexpected error reading folder: ${e.message}", e)
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
            title = title,
            isLocal = true
        )
    }
}

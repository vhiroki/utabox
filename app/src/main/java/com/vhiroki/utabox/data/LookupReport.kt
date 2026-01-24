package com.vhiroki.utabox.data

/**
 * Report of a song lookup operation, containing details about
 * which folders were scanned, how many songs were found, and any errors.
 */
data class LookupReport(
    val localReport: SourceReport?,
    val youtubeReport: SourceReport?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalSongs: Int
        get() = (localReport?.songCount ?: 0) + (youtubeReport?.songCount ?: 0)

    val hasErrors: Boolean
        get() = localReport?.error != null || youtubeReport?.error != null

    val isSuccess: Boolean
        get() = totalSongs > 0
}

/**
 * Report for a single source (USB/local or YouTube).
 */
data class SourceReport(
    val sourceName: String,
    val folderPath: String?,
    val csvFiles: List<CsvFileReport>,
    val songCount: Int,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = error == null && songCount > 0
}

/**
 * Report for a single CSV file that was processed.
 */
data class CsvFileReport(
    val filename: String,
    val songCount: Int,
    val error: String? = null
)

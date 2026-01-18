package com.vhiroki.utabox.data

import java.text.Normalizer

data class Song(
    val code: String,
    val filename: String,
    val artist: String,
    val title: String,
    val youtubeUrl: String? = null,
    val notes: String? = null,
    val isLocal: Boolean = false
) {
    val isYouTube: Boolean get() = youtubeUrl != null

    // Pre-computed normalized values for fast search (computed once at creation)
    val codeLower: String by lazy { code.lowercase() }
    val titleNormalized: String by lazy { title.lowercase().removeDiacritics() }
    val artistNormalized: String by lazy { artist.lowercase().removeDiacritics() }

    companion object {
        /**
         * Removes diacritics (accents) from a string for accent-insensitive comparison.
         * For example: "pão" becomes "pao", "café" becomes "cafe"
         */
        fun String.removeDiacritics(): String {
            val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
            return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }
    }
}

package com.vhiroki.utabox.data

data class Song(
    val code: String,
    val filename: String,
    val artist: String,
    val title: String,
    val youtubeUrl: String? = null
) {
    val isYouTube: Boolean get() = youtubeUrl != null
}

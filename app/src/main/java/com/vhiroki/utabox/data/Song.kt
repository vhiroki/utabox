package com.vhiroki.utabox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val musicId: String,
    val artista: String,
    val musica: String,
    val inicio: String
)

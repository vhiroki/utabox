package com.vhiroki.utabox.data

import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun searchSongs(query: String): Flow<List<Song>> {
        return if (query.isBlank()) {
            songDao.getAllSongs()
        } else {
            songDao.searchSongs(query.trim())
        }
    }

    suspend fun getSongCount(): Int = songDao.getCount()
}

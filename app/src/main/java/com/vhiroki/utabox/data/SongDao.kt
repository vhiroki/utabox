package com.vhiroki.utabox.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY artista, musica")
    fun getAllSongs(): Flow<List<Song>>

    @Query("""
        SELECT * FROM songs 
        WHERE musicId LIKE :query || '%'
           OR musica LIKE '%' || :query || '%'
           OR artista LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN musicId LIKE :query || '%' THEN 0 ELSE 1 END,
            artista, musica
    """)
    fun searchSongs(query: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getCount(): Int
}

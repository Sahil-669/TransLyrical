package com.example.translyrical.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongCacheDao {
    @Query("SELECT * FROM song_translations WHERE songId = :songId LIMIT 1")
    suspend fun getTranslationCache(songId: String): SongCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: SongCacheEntity)
}
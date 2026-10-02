package com.example.translyrical.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.lang.System.currentTimeMillis

@Entity(tableName = "recent_songs")
data class RecentSong(
    @PrimaryKey val uniqueId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val timestamp: Long = currentTimeMillis()
)

@Dao
interface RecentSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(song: RecentSong)

    @Query("SELECT * FROM recent_songs ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSongs(): Flow<List<RecentSong>>
}
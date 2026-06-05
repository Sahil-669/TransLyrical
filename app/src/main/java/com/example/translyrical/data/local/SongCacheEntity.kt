package com.example.translyrical.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_translations")
data class SongCacheEntity(
    @PrimaryKey val songId: String,
    val translatedJson: String
)

package com.example.translyrical.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SlangEntity::class, SongCacheEntity::class], version = 2, exportSchema = false)
abstract class SlangDatabase: RoomDatabase() {
    abstract val slangDao: SlangDao
    abstract val songCacheDao: SongCacheDao
}
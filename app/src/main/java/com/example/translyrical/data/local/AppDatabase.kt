package com.example.translyrical.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecentSong::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun recentSongDao(): RecentSongDao
}
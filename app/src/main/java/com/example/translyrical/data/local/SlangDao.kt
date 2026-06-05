package com.example.translyrical.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SlangDao {
    @Query("SELECT standard FROM slang_dictionary WHERE slang = :slang LIMIT 1")
    suspend fun getStandardWord(slang: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slangs: List<SlangEntity>)

    @Query("SELECT COUNT(*) FROM slang_dictionary")
    suspend fun getDictionarySize(): Int
}
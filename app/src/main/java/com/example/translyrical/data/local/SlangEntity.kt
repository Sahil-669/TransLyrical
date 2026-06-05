package com.example.translyrical.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "slang_dictionary")
data class SlangEntity(
    @PrimaryKey val slang: String,
    val standard: String
)
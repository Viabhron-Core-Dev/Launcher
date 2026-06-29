package com.example.templatelauncher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey val packageName: String,
    val isHidden: Boolean,
    val customLabel: String?
)

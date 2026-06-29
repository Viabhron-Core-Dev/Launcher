package com.example.templatelauncher.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_items")
data class WorkspaceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val page: Int,
    val col: Int,
    val row: Int
)

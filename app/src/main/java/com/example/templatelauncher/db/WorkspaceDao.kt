package com.example.templatelauncher.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_items")
    suspend fun getAllWorkspaceItems(): List<WorkspaceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceItem(item: WorkspaceItem): Long

    @Update
    suspend fun updateWorkspaceItem(item: WorkspaceItem)

    @Delete
    suspend fun deleteWorkspaceItem(item: WorkspaceItem)

    @Query("DELETE FROM workspace_items WHERE page = :page AND col = :col AND row = :row")
    suspend fun deleteWorkspaceItemAt(page: Int, col: Int, row: Int)

    @Query("SELECT * FROM app_preferences WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppPreference(packageName: String): AppPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppPreference(pref: AppPreference)

    @Query("SELECT * FROM app_preferences")
    suspend fun getAllAppPreferences(): List<AppPreference>
}

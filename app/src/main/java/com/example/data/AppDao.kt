package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM created_apps ORDER BY createdAt DESC")
    fun getAllApps(): Flow<List<CreatedApp>>

    @Query("SELECT * FROM created_apps WHERE id = :id LIMIT 1")
    suspend fun getAppById(id: Int): CreatedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: CreatedApp): Long

    @Update
    suspend fun updateApp(app: CreatedApp)

    @Delete
    suspend fun deleteApp(app: CreatedApp)

    @Query("DELETE FROM created_apps WHERE id = :id")
    suspend fun deleteAppById(id: Int)
}

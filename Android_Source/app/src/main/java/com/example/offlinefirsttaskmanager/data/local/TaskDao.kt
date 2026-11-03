
package com.example.offlinefirsttaskmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE id IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun countSynced(ids: List<String>): Int

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE syncStatus IN ('PENDING', 'FAILED', 'SYNCING', 'CONFLICT')
        ORDER BY updatedAt ASC
    """)
    suspend fun getPendingForSync(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("""
        UPDATE tasks
        SET isDeleted = 1,
            syncStatus = 'PENDING',
            updatedAt = :time
        WHERE id = :id
    """)
    suspend fun deleteTask(id: String, time: Long)

    @Query("""
        UPDATE tasks
        SET syncStatus = 'SYNCING'
        WHERE id = :id
          AND updatedAt = :updatedAt
          AND syncStatus IN ('PENDING', 'FAILED', 'SYNCING', 'CONFLICT')
    """)
    suspend fun markSyncing(
        id: String,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE tasks
        SET serverVersion = :newVersion,
            syncStatus = CASE
                WHEN updatedAt = :sentUpdatedAt THEN 'SYNCED'
                ELSE 'PENDING'
            END
        WHERE id = :id
          AND serverVersion <= :newVersion
    """)
    suspend fun confirmSynced(
        id: String,
        sentUpdatedAt: Long,
        newVersion: Long
    ): Int


    @Query("""
        UPDATE tasks
        SET serverVersion = :newVersion,
            syncStatus = CASE
                WHEN updatedAt = :sentUpdatedAt THEN 'SYNCED'
                ELSE 'PENDING'
            END
        WHERE id = :id
          AND serverVersion = :previousVersion
          AND syncStatus IN ('SYNCING', 'PENDING')
    """)
    suspend fun confirmRecreated(
        id: String,
        sentUpdatedAt: Long,
        previousVersion: Long,
        newVersion: Long
    ): Int

    @Query("""
        UPDATE tasks
        SET serverVersion = 0,
            syncStatus = CASE
                WHEN updatedAt = :sentUpdatedAt THEN 'SYNCED'
                ELSE 'PENDING'
            END
        WHERE id = :id
          AND isDeleted = 1
          AND serverVersion = :previousVersion
    """)
    suspend fun confirmMissingRemoteDeletion(
        id: String,
        sentUpdatedAt: Long,
        previousVersion: Long
    ): Int

    @Query("""
        UPDATE tasks
        SET syncStatus = 'FAILED'
        WHERE id = :id
          AND updatedAt = :updatedAt
          AND syncStatus = 'SYNCING'
    """)
    suspend fun markFailed(
        id: String,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE tasks
        SET syncStatus = 'CONFLICT'
        WHERE id = :id
          AND updatedAt = :updatedAt
          AND syncStatus = 'SYNCING'
    """)
    suspend fun markConflict(
        id: String,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE tasks
        SET syncStatus = 'SYNCED'
        WHERE id = :id
          AND updatedAt = :updatedAt
          AND isDeleted = 1
          AND serverVersion = 0
    """)
    suspend fun markLocalDeleteSynced(
        id: String,
        updatedAt: Long
    ): Int
}

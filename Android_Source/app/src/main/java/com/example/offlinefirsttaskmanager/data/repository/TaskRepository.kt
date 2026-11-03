
package com.example.offlinefirsttaskmanager.data.repository

import android.content.Context
import com.example.offlinefirsttaskmanager.data.local.TaskDao
import com.example.offlinefirsttaskmanager.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val appContext: Context
) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    private fun nextUpdatedAt(task: TaskEntity): Long {
        return maxOf(
            System.currentTimeMillis(),
            task.updatedAt + 1
        )
    }

    private fun requestAutoSync() {
        SyncScheduler.enqueue(appContext)
    }

    suspend fun addTask(
        title: String,
        description: String = ""
    ): Boolean {

        if (title.isBlank()) return false

        val task = TaskEntity(
            title = title.trim(),
            description = description.trim(),
            syncStatus = "PENDING"
        )

        taskDao.insertTask(task)
        requestAutoSync()
        return true
    }

    suspend fun updateTask(
        id: String,
        title: String,
        description: String
    ): Boolean {

        if (title.isBlank()) return false

        val oldTask = taskDao.getTaskById(id) ?: return false
        if (oldTask.isDeleted) return false

        val updatedTask = oldTask.copy(
            title = title.trim(),
            description = description.trim(),
            updatedAt = nextUpdatedAt(oldTask),
            syncStatus = "PENDING"
        )

        taskDao.updateTask(updatedTask)
        requestAutoSync()
        return true
    }

    suspend fun toggleTaskCompletion(id: String): Boolean {

        val task = taskDao.getTaskById(id) ?: return false
        if (task.isDeleted) return false

        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = nextUpdatedAt(task),
            syncStatus = "PENDING"
        )

        taskDao.updateTask(updatedTask)
        requestAutoSync()
        return true
    }

    suspend fun deleteTask(id: String): Boolean {

        val task = taskDao.getTaskById(id) ?: return false
        if (task.isDeleted) return false

        taskDao.deleteTask(
            id,
            nextUpdatedAt(task)
        )

        requestAutoSync()
        return true
    }

    suspend fun getTaskById(id: String): TaskEntity? {
        return taskDao.getTaskById(id)
    }

    suspend fun getPendingTasks(): List<TaskEntity> {
        return taskDao.getPendingTasks()
    }
}

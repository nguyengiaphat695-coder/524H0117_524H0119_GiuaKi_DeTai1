
package com.example.offlinefirsttaskmanager.data.repository

import androidx.room.withTransaction
import com.example.offlinefirsttaskmanager.data.local.AppDatabase
import com.example.offlinefirsttaskmanager.data.local.TaskEntity
import com.example.offlinefirsttaskmanager.data.remote.CreateTaskRequest
import com.example.offlinefirsttaskmanager.data.remote.TaskApi
import com.example.offlinefirsttaskmanager.data.remote.TaskResponse
import com.example.offlinefirsttaskmanager.data.remote.UpdateTaskRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int,
    val errorMessage: String?,
    val pendingBefore: Int = 0,
    val pendingCompleted: Int = 0,
    val pullSucceeded: Boolean = true
)

class SyncRepository(
    private val database: AppDatabase,
    private val api: TaskApi
) {
    private val taskDao = database.taskDao()

    companion object {
        private val sharedMutex = Mutex()
    }

    private enum class ConflictOutcome {
        LOCAL_WINS,
        SERVER_WINS,
        SKIPPED,
        UNRESOLVED
    }

    private fun TaskResponse.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            syncStatus = "SYNCED",
            serverVersion = serverVersion
        )
    }

    private suspend fun resolveConflict(
        task: TaskEntity
    ): ConflictOutcome {
        repeat(3) {
            val remote = api.getTask(task.id)

            val serverWins =
                (remote.isDeleted && !task.isDeleted) ||
                        (remote.updatedAt >= task.updatedAt)

            if (serverWins) {
                val applied = database.withTransaction {
                    val latest = taskDao.getTaskById(task.id)

                    if (
                        latest != null &&
                        latest.updatedAt == task.updatedAt &&
                        latest.syncStatus == "SYNCING"
                    ) {
                        taskDao.insertTask(remote.toEntity())
                        true
                    } else {
                        false
                    }
                }

                return if (applied) {
                    ConflictOutcome.SERVER_WINS
                } else {
                    ConflictOutcome.SKIPPED
                }
            }

            try {
                val operation = if (task.isDeleted) {
                    "delete"
                } else {
                    "update"
                }

                val key =
                    "resolve-${task.id}-${task.updatedAt}-${remote.serverVersion}-$operation"

                val response = if (task.isDeleted) {
                    api.deleteTask(
                        task.id,
                        remote.serverVersion,
                        key
                    )
                } else {
                    api.updateTask(
                        task.id,
                        UpdateTaskRequest(
                            title = task.title,
                            description = task.description,
                            isCompleted = task.isCompleted,
                            expectedVersion = remote.serverVersion
                        ),
                        key
                    )
                }

                taskDao.confirmSynced(
                    task.id,
                    task.updatedAt,
                    response.serverVersion
                )

                return ConflictOutcome.LOCAL_WINS
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() != 409) {
                    throw e
                }
            }
        }

        return ConflictOutcome.UNRESOLVED
    }

    suspend fun syncNow(): SyncResult = sharedMutex.withLock {
        var uploaded = 0
        var downloaded = 0
        var conflicts = 0
        var pullSucceeded = true

        val errors = mutableListOf<String>()
        val pendingTasks = taskDao.getPendingForSync()

        for (task in pendingTasks) {
            if (
                taskDao.markSyncing(
                    task.id,
                    task.updatedAt
                ) == 0
            ) {
                continue
            }

            if (task.isDeleted && task.serverVersion == 0L) {
                try {
                    val remote = api.getTask(task.id)

                    if (remote.isDeleted) {
                        taskDao.confirmSynced(
                            task.id,
                            task.updatedAt,
                            remote.serverVersion
                        )
                    } else {
                        val response = api.deleteTask(
                            task.id,
                            remote.serverVersion,
                            "task-${task.id}-${task.updatedAt}-recover-delete"
                        )

                        taskDao.confirmSynced(
                            task.id,
                            task.updatedAt,
                            response.serverVersion
                        )
                        uploaded++
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: HttpException) {
                    if (e.code() == 404) {
                        taskDao.markLocalDeleteSynced(
                            task.id,
                            task.updatedAt
                        )
                    } else {
                        taskDao.markFailed(task.id, task.updatedAt)
                        if (e.code() == 409) conflicts++
                        errors.add("Kiem tra xoa: HTTP ${e.code()}")
                    }
                } catch (e: IOException) {
                    taskDao.markFailed(task.id, task.updatedAt)
                    errors.add("Mat ket noi khi xac nhan xoa")
                    break
                } catch (e: Exception) {
                    taskDao.markFailed(task.id, task.updatedAt)
                    errors.add("Khong the xac nhan xoa: ${e.message}")
                }
                continue
            }

            try {
                val operation = when {
                    task.isDeleted -> "delete"
                    task.serverVersion == 0L -> "create"
                    else -> "update"
                }

                val key =
                    "task-${task.id}-${task.updatedAt}-$operation"

                val response = when {
                    task.isDeleted -> {
                        api.deleteTask(
                            task.id,
                            task.serverVersion,
                            key
                        )
                    }

                    task.serverVersion == 0L -> {
                        api.createTask(
                            CreateTaskRequest(
                                id = task.id,
                                title = task.title,
                                description = task.description,
                                isCompleted = task.isCompleted
                            ),
                            key
                        )
                    }

                    else -> {
                        api.updateTask(
                            task.id,
                            UpdateTaskRequest(
                                title = task.title,
                                description = task.description,
                                isCompleted = task.isCompleted,
                                expectedVersion = task.serverVersion
                            ),
                            key
                        )
                    }
                }

                taskDao.confirmSynced(
                    task.id,
                    task.updatedAt,
                    response.serverVersion
                )

                uploaded++
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    conflicts++

                    try {
                        when (resolveConflict(task)) {
                            ConflictOutcome.LOCAL_WINS -> {
                                uploaded++
                            }

                            ConflictOutcome.SERVER_WINS -> {
                                downloaded++
                            }

                            ConflictOutcome.SKIPPED -> {
                                Unit
                            }

                            ConflictOutcome.UNRESOLVED -> {
                                taskDao.markConflict(
                                    task.id,
                                    task.updatedAt
                                )
                                errors.add(
                                    "Chua xu ly duoc xung dot: ${task.title}"
                                )
                            }
                        }
                    } catch (conflictError: CancellationException) {
                        throw conflictError
                    } catch (conflictError: Exception) {
                        taskDao.markFailed(
                            task.id,
                            task.updatedAt
                        )

                        errors.add(
                            "Loi xu ly xung dot: ${conflictError.message}"
                        )

                        if (conflictError is IOException) {
                            break
                        }
                    }
                } else if (e.code() == 404) {
                    try {
                        if (task.isDeleted) {
                            taskDao.confirmMissingRemoteDeletion(
                                task.id,
                                task.updatedAt,
                                task.serverVersion
                            )
                        } else {
                            val recreated = api.createTask(
                                CreateTaskRequest(
                                    id = task.id,
                                    title = task.title,
                                    description = task.description,
                                    isCompleted = task.isCompleted
                                ),
                                "task-${task.id}-${task.updatedAt}-restore"
                            )
                            taskDao.confirmRecreated(
                                task.id,
                                task.updatedAt,
                                task.serverVersion,
                                recreated.serverVersion
                            )
                            uploaded++
                        }
                    } catch (restoreError: CancellationException) {
                        throw restoreError
                    } catch (restoreError: Exception) {
                        taskDao.markFailed(task.id, task.updatedAt)
                        if (restoreError is HttpException && restoreError.code() == 409) {
                            conflicts++
                        }
                        errors.add("Khoi phuc ban ghi: ${restoreError.message}")
                        if (restoreError is IOException) break
                    }
                } else {
                    taskDao.markFailed(
                        task.id,
                        task.updatedAt
                    )
                    errors.add(
                        "Gui du lieu: HTTP ${e.code()}"
                    )
                }
            } catch (e: IOException) {
                taskDao.markFailed(
                    task.id,
                    task.updatedAt
                )
                errors.add(
                    "Mat ket noi khi gui du lieu"
                )
                break
            } catch (e: Exception) {
                taskDao.markFailed(
                    task.id,
                    task.updatedAt
                )
                errors.add(
                    "Khong the gui ${task.title}: ${e.message}"
                )
            }
        }

        try {
            val remoteTasks = api.getTasks()

            for (remote in remoteTasks) {
                val applied = database.withTransaction {
                    val local = taskDao.getTaskById(remote.id)

                    if (
                        local == null ||
                        (
                                local.syncStatus == "SYNCED" &&
                                        remote.serverVersion > local.serverVersion
                                )
                    ) {
                        taskDao.insertTask(remote.toEntity())
                        true
                    } else {
                        false
                    }
                }

                if (applied) {
                    downloaded++
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            pullSucceeded = false
            errors.add(
                "Tai du lieu: HTTP ${e.code()}"
            )
        } catch (e: IOException) {
            pullSucceeded = false
            errors.add(
                "Mat ket noi khi tai du lieu"
            )
        } catch (e: Exception) {
            pullSucceeded = false
            errors.add(
                "Khong the tai du lieu: ${e.message}"
            )
        }

        val pendingIds = pendingTasks.map { it.id }
        val completed = if (pendingIds.isEmpty()) 0 else taskDao.countSynced(pendingIds)

        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            conflicts = conflicts,
            pendingBefore = pendingTasks.size,
            pendingCompleted = completed,
            pullSucceeded = pullSucceeded,
            errorMessage = errors
                .distinct()
                .joinToString("; ")
                .takeIf { it.isNotBlank() }
        )
    }
}

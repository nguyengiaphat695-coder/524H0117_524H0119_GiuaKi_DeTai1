
package com.example.offlinefirsttaskmanager.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offlinefirsttaskmanager.data.local.TaskEntity
import com.example.offlinefirsttaskmanager.data.repository.SyncRepository
import com.example.offlinefirsttaskmanager.data.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class SyncUiState(
    val isSyncing: Boolean = false,
    val message: String = ""
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val allTasks: StateFlow<List<TaskEntity>> =
        repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _syncUiState = MutableStateFlow(SyncUiState())

    val syncUiState: StateFlow<SyncUiState> =
        _syncUiState.asStateFlow()

    fun addTask(
        title: String,
        description: String = ""
    ) {
        viewModelScope.launch {
            repository.addTask(title, description)
        }
    }

    fun updateTask(
        id: String,
        title: String,
        description: String
    ) {
        viewModelScope.launch {
            repository.updateTask(id, title, description)
        }
    }

    fun toggleTaskCompletion(id: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(id)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun syncNow() {
        if (_syncUiState.value.isSyncing) return

        _syncUiState.value = SyncUiState(
            isSyncing = true,
            message = "Đang đồng bộ dữ liệu..."
        )

        viewModelScope.launch {
            val startTime = SystemClock.elapsedRealtime()

            try {
                val result = syncRepository.syncNow()

                val durationMs =
                    SystemClock.elapsedRealtime() - startTime

                val durationSeconds = durationMs / 1000.0

                val timeText = String.format(
                    Locale.US,
                    "%.2f",
                    durationSeconds
                )

                val completionRate = if (result.pendingBefore > 0) {
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        result.pendingCompleted * 100.0 / result.pendingBefore
                    )
                } else {
                    "Không có bản ghi chờ"
                }

                val summary =
                    "Gửi lên: ${result.uploaded}, " +
                            "Tải về: ${result.downloaded}, " +
                            "Xung đột: ${result.conflicts}, " +
                            "Hoàn tất ${result.pendingCompleted}/${result.pendingBefore} " +
                            "bản ghi chờ ($completionRate)"

                val message = when {
                    result.errorMessage != null ->
                        "Đồng bộ chưa hoàn tất. $summary. " +
                                result.errorMessage

                    result.conflicts > 0 ->
                        "Đã phát hiện và xử lý ${result.conflicts} " +
                                "xung đột dữ liệu. $summary"

                    else ->
                        "Đợt đồng bộ hoàn tất! $summary"
                }

                val finalMessage =
                    "$message\nThời gian đồng bộ: $timeText giây"

                _syncUiState.value = SyncUiState(
                    isSyncing = false,
                    message = finalMessage
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                val durationMs =
                    SystemClock.elapsedRealtime() - startTime

                val timeText = String.format(
                    Locale.US,
                    "%.2f",
                    durationMs / 1000.0
                )

                _syncUiState.value = SyncUiState(
                    isSyncing = false,
                    message =
                        "Đồng bộ thất bại: ${e.message}\n" +
                                "Thời gian thực hiện: $timeText giây"
                )
            }
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val syncRepository: SyncRepository
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            if (
                modelClass.isAssignableFrom(
                    TaskViewModel::class.java
                )
            ) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(
                    repository,
                    syncRepository
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}

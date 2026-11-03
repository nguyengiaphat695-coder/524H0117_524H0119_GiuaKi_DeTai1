
package com.example.offlinefirsttaskmanager

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinefirsttaskmanager.data.local.AppDatabase
import com.example.offlinefirsttaskmanager.data.local.TaskEntity
import com.example.offlinefirsttaskmanager.data.remote.RetrofitClient
import com.example.offlinefirsttaskmanager.data.repository.SyncRepository
import com.example.offlinefirsttaskmanager.data.repository.TaskRepository
import com.example.offlinefirsttaskmanager.ui.TaskAdapter
import com.example.offlinefirsttaskmanager.ui.TaskViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.offlinefirsttaskmanager.data.repository.SyncScheduler


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        val database = AppDatabase.getDatabase(
            applicationContext
        )

        val repository = TaskRepository(
            database.taskDao(),
            applicationContext
        )

        SyncScheduler.startPeriodic(applicationContext)
        SyncScheduler.enqueue(applicationContext)


        val syncRepository = SyncRepository(
            database,
            RetrofitClient.api
        )

        viewModel = ViewModelProvider(
            this,
            TaskViewModel.Factory(
                repository,
                syncRepository
            )
        )[TaskViewModel::class.java]

        val edtTitle = findViewById<EditText>(
            R.id.edtTitle
        )

        val edtDescription = findViewById<EditText>(
            R.id.edtDescription
        )

        val btnAddTask = findViewById<Button>(
            R.id.btnAddTask
        )

        val btnCheckServer = findViewById<Button>(
            R.id.btnCheckServer
        )

        val btnSyncNow = findViewById<Button>(
            R.id.btnSyncNow
        )

        val tvSyncInfo = findViewById<TextView>(
            R.id.tvSyncInfo
        )

        val tvTaskCount = findViewById<TextView>(
            R.id.tvTaskCount
        )

        val tvEmptyState = findViewById<TextView>(
            R.id.tvEmptyState
        )

        val rvTasks = findViewById<RecyclerView>(
            R.id.rvTasks
        )

        taskAdapter = TaskAdapter(
            onToggle = { id ->
                viewModel.toggleTaskCompletion(id)
            },
            onEdit = { task ->
                showEditDialog(task)
            },
            onDelete = { id ->
                showDeleteDialog(id)
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = taskAdapter

        btnAddTask.setOnClickListener {
            val title = edtTitle.text.toString().trim()

            val description =
                edtDescription.text.toString().trim()

            if (title.isBlank()) {
                edtTitle.error = "Vui lòng nhập tên công việc"
                edtTitle.requestFocus()
                return@setOnClickListener
            }

            viewModel.addTask(title, description)

            edtTitle.text.clear()
            edtDescription.text.clear()

            Toast.makeText(
                this,
                "Đã thêm công việc vào Room",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnCheckServer.setOnClickListener {
            btnCheckServer.isEnabled = false
            tvSyncInfo.text = "Đang kiểm tra FastAPI..."

            lifecycleScope.launch {
                try {
                    val health = RetrofitClient.api.checkHealth()

                    if (health.status == "ok") {
                        tvSyncInfo.text =
                            "KẾT NỐI SERVER THÀNH CÔNG!"
                    } else {
                        tvSyncInfo.text =
                            "Server trả về: ${health.status}"
                    }

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    tvSyncInfo.text =
                        "Lỗi kết nối: ${e.message}"

                } finally {
                    btnCheckServer.isEnabled = true
                }
            }
        }

        btnSyncNow.setOnClickListener {
            viewModel.syncNow()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.allTasks.collect { tasks ->
                        taskAdapter.submitList(tasks)

                        tvTaskCount.text =
                            "Danh sách công việc (${tasks.size})"

                        tvEmptyState.visibility =
                            if (tasks.isEmpty()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }

                launch {
                    viewModel.syncUiState.collect { state ->
                        btnSyncNow.isEnabled = !state.isSyncing

                        btnSyncNow.text =
                            if (state.isSyncing) {
                                "ĐANG ĐỒNG BỘ..."
                            } else {
                                "ĐỒNG BỘ NGAY"
                            }

                        if (state.message.isNotBlank()) {
                            tvSyncInfo.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun showEditDialog(task: TaskEntity) {

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val padding =
                (20 * resources.displayMetrics.density).toInt()

            setPadding(padding, padding, padding, 0)
        }

        val edtTitle = EditText(this).apply {
            hint = "Tên công việc"
            setText(task.title)
            setSingleLine(true)
        }

        val edtDescription = EditText(this).apply {
            hint = "Mô tả công việc"
            setText(task.description)
            minLines = 2
        }

        container.addView(edtTitle)
        container.addView(edtDescription)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Chỉnh sửa công việc")
            .setView(container)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)
            .create()

        dialog.show()

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener {

            val title = edtTitle.text.toString().trim()

            val description =
                edtDescription.text.toString().trim()

            if (title.isBlank()) {
                edtTitle.error = "Vui lòng nhập tên công việc"
                edtTitle.requestFocus()
            } else {
                viewModel.updateTask(
                    task.id,
                    title,
                    description
                )

                Toast.makeText(
                    this,
                    "Đã cập nhật công việc",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
        }
    }

    private fun showDeleteDialog(id: String) {

        AlertDialog.Builder(this)
            .setTitle("Xóa công việc")
            .setMessage(
                "Bạn có chắc muốn xóa công việc này?"
            )
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->

                viewModel.deleteTask(id)

                Toast.makeText(
                    this,
                    "Đã xóa công việc",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }
}

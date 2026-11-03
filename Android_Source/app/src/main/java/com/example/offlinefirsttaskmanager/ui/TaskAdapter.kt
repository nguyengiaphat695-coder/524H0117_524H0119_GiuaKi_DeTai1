
package com.example.offlinefirsttaskmanager.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinefirsttaskmanager.R
import com.example.offlinefirsttaskmanager.data.local.TaskEntity

class TaskAdapter(
    private val onToggle: (String) -> Unit,
    private val onEdit: (TaskEntity) -> Unit,
    private val onDelete: (String) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(
    TaskDiffCallback()
) {

    inner class TaskViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val cbCompleted: CheckBox =
            view.findViewById(R.id.cbCompleted)

        val tvDescription: TextView =
            view.findViewById(R.id.tvDescription)

        val tvSyncStatus: TextView =
            view.findViewById(R.id.tvSyncStatus)

        val btnEdit: Button =
            view.findViewById(R.id.btnEdit)

        val btnDelete: Button =
            view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)

        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TaskViewHolder,
        position: Int
    ) {
        val task = getItem(position)

        holder.cbCompleted.setOnCheckedChangeListener(null)

        holder.cbCompleted.text = task.title
        holder.cbCompleted.isChecked = task.isCompleted

        val currentFlags = holder.cbCompleted.paintFlags

        holder.cbCompleted.paintFlags =
            if (task.isCompleted) {
                currentFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                currentFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

        holder.cbCompleted.setOnCheckedChangeListener {
                _, isChecked ->
            if (isChecked != task.isCompleted) {
                onToggle(task.id)
            }
        }

        holder.tvDescription.text = task.description

        holder.tvDescription.visibility =
            if (task.description.isBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        holder.tvSyncStatus.text = when (task.syncStatus) {
            "PENDING" -> "Chưa đồng bộ"
            "SYNCING" -> "Đang đồng bộ"
            "SYNCED" -> "Đã đồng bộ"
            "FAILED" -> "Đồng bộ thất bại"
            "CONFLICT" -> "Xung đột dữ liệu"
            else -> task.syncStatus
        }

        holder.btnEdit.setOnClickListener {
            onEdit(task)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(task.id)
        }
    }

    private class TaskDiffCallback :
        DiffUtil.ItemCallback<TaskEntity>() {

        override fun areItemsTheSame(
            oldItem: TaskEntity,
            newItem: TaskEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TaskEntity,
            newItem: TaskEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}

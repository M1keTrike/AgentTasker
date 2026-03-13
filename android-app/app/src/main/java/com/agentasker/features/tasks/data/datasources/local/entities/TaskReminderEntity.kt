package com.agentasker.features.tasks.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_reminders")
data class TaskReminderEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val description: String,
    val reminderAt: Long
)

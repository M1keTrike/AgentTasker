package com.agentasker.features.tasks.domain.repositories

interface TaskReminderRepository {
    suspend fun saveReminder(taskId: String, title: String, description: String, reminderAt: Long)
    suspend fun deleteReminder(taskId: String)
    suspend fun getReminderAt(taskId: String): Long?
}

package com.agentasker.features.tasks.data.datasources.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agentasker.features.tasks.data.datasources.local.entities.TaskReminderEntity

@Dao
interface TaskReminderDao {

    @Upsert
    suspend fun upsertReminder(reminder: TaskReminderEntity)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteReminder(taskId: String)

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId")
    suspend fun getReminderForTask(taskId: String): TaskReminderEntity?

    @Query("SELECT * FROM task_reminders WHERE reminderAt > :now")
    suspend fun getActiveReminders(now: Long = System.currentTimeMillis()): List<TaskReminderEntity>
}

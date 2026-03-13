package com.agentasker.features.tasks.data.repositories

import com.agentasker.features.tasks.data.datasources.local.dao.TaskReminderDao
import com.agentasker.features.tasks.data.datasources.local.entities.TaskReminderEntity
import com.agentasker.features.tasks.domain.repositories.TaskReminderRepository
import javax.inject.Inject

class TaskReminderRepositoryImpl @Inject constructor(
    private val taskReminderDao: TaskReminderDao
) : TaskReminderRepository {

    override suspend fun saveReminder(taskId: String, title: String, description: String, reminderAt: Long) {
        taskReminderDao.upsertReminder(
            TaskReminderEntity(
                taskId = taskId,
                title = title,
                description = description,
                reminderAt = reminderAt
            )
        )
    }

    override suspend fun deleteReminder(taskId: String) {
        taskReminderDao.deleteReminder(taskId)
    }

    override suspend fun getReminderAt(taskId: String): Long? {
        return taskReminderDao.getReminderForTask(taskId)?.reminderAt
    }
}

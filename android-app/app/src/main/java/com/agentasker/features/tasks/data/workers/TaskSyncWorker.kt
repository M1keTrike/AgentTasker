package com.agentasker.features.tasks.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntity
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TaskSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: AgentTaskerApi,
    private val taskDao: TaskDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingTasks = taskDao.getPendingTasks()

        if (pendingTasks.isEmpty()) {
            return Result.success()
        }

        var hasFailures = false

        for (task in pendingTasks) {
            try {
                when (task.pendingAction) {
                    "create" -> {
                        val request = CreateTaskRequest(
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            status = task.status,
                            dueDate = task.dueDate
                        )
                        val response = api.createTask(request)
                        val syncedEntity = response.toEntity(isSynced = true)
                        taskDao.deleteTaskById(task.id)
                        taskDao.upsertTask(syncedEntity)
                    }

                    "update" -> {
                        val request = UpdateTaskRequest(
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            status = task.status,
                            dueDate = task.dueDate
                        )
                        val response = api.updateTask(task.id.toInt(), request)
                        val syncedEntity = response.toEntity(isSynced = true)
                        taskDao.upsertTask(syncedEntity)
                    }

                    "delete" -> {
                        api.deleteTask(task.id.toInt())
                        taskDao.deleteTaskById(task.id)
                    }
                }
            } catch (_: Exception) {
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        const val WORK_NAME = "task_sync_worker"
    }
}

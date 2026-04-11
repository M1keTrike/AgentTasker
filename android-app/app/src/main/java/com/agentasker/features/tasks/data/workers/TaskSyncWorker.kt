package com.agentasker.features.tasks.data.workers

import android.content.Context
import android.util.Log
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
import retrofit2.HttpException

@HiltWorker
class TaskSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: AgentTaskerApi,
    private val taskDao: TaskDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingTasks = taskDao.getPendingTasks()
        Log.d(TAG, "doWork started — pending=${pendingTasks.size}")

        if (pendingTasks.isEmpty()) {
            return Result.success()
        }

        var hasFailures = false

        for (task in pendingTasks) {
            try {
                Log.d(
                    TAG,
                    "Processing task id=${task.id} action=${task.pendingAction} " +
                        "title=${task.title} dueDate=${task.dueDate}",
                )
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
                        Log.d(TAG, "  → created remote id=${response.id}")
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
                        Log.d(TAG, "  → updated remote id=${response.id}")
                    }

                    "delete" -> {
                        api.deleteTask(task.id.toInt())
                        taskDao.deleteTaskById(task.id)
                        Log.d(TAG, "  → deleted remote id=${task.id}")
                    }
                }
            } catch (e: HttpException) {
                // Log DETALLADO del error HTTP para poder diagnosticar 4xx/5xx.
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(
                    TAG,
                    "HTTP ${e.code()} syncing task id=${task.id} action=${task.pendingAction}: $body",
                    e,
                )
                hasFailures = true
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Exception syncing task id=${task.id} action=${task.pendingAction}: ${e.message}",
                    e,
                )
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "TaskSyncWorker"
        const val WORK_NAME = "task_sync_worker"
    }
}

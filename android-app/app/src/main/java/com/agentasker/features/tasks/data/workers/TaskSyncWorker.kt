package com.agentasker.features.tasks.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.tasks.data.datasources.local.dao.SubtaskDao
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.entities.SubtaskEntity
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntity
import com.agentasker.features.tasks.data.datasources.remote.model.BulkCreateSubtasksRequest
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateSubtaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException

@HiltWorker
class TaskSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: AgentTaskerApi,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingTasks = taskDao.getPendingTasks()
        val pendingSubtasks = subtaskDao.getPending()
        Log.d(
            TAG,
            "doWork started — pendingTasks=${pendingTasks.size} pendingSubtasks=${pendingSubtasks.size}"
        )

        if (pendingTasks.isEmpty() && pendingSubtasks.isEmpty()) {
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
                            dueDate = task.dueDate,
                            source = task.source,
                            externalId = task.externalId,
                            courseName = task.courseName,
                            externalLink = task.externalLink
                        )
                        val oldId = task.id
                        val orphanSubtasks = subtaskDao.getByTaskId(oldId)

                        val response = api.createTask(request)
                        val syncedEntity = response.toEntity(isSynced = true)
                        taskDao.deleteTaskById(oldId)
                        taskDao.upsertTask(syncedEntity)

                        if (orphanSubtasks.isNotEmpty()) {
                            val reassigned = orphanSubtasks.map {
                                it.copy(
                                    id = "local_sub_${System.nanoTime()}_${it.position}",
                                    taskId = syncedEntity.id,
                                    pendingAction = "create",
                                    isSynced = false
                                )
                            }
                            subtaskDao.upsertAll(reassigned)
                        }
                        Log.d(TAG, "  → created remote id=${response.id} (+${orphanSubtasks.size} subtasks queued)")
                    }

                    "update" -> {
                        val request = UpdateTaskRequest(
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            status = task.status,
                            dueDate = task.dueDate,
                            isArchived = task.isArchived
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

        val subsToSync = subtaskDao.getPending()

        val createsByTask = subsToSync
            .filter { it.pendingAction == "create" && it.taskId.toIntOrNull() != null }
            .groupBy { it.taskId }

        for ((taskId, subs) in createsByTask) {
            try {
                val titles = subs.sortedBy { it.position }.map { it.title }
                val response = api.createSubtasksBulk(
                    taskId.toInt(),
                    BulkCreateSubtasksRequest(subtasks = titles)
                )
                subs.forEach { subtaskDao.deleteById(it.id) }
                val synced = response.map { dto ->
                    SubtaskEntity(
                        id = dto.id.toString(),
                        taskId = taskId,
                        title = dto.title,
                        isCompleted = dto.isCompleted,
                        position = dto.position,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isSynced = true
                    )
                }
                subtaskDao.upsertAll(synced)
                Log.d(TAG, "  → bulk created ${synced.size} subtasks for taskId=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "Exception bulk-creating subtasks for taskId=$taskId: ${e.message}", e)
                hasFailures = true
            }
        }

        for (sub in subsToSync.filter { it.pendingAction == "update" && it.id.toIntOrNull() != null }) {
            try {
                val response = api.updateSubtask(
                    sub.id.toInt(),
                    UpdateSubtaskRequest(title = sub.title, isCompleted = sub.isCompleted)
                )
                subtaskDao.upsert(
                    sub.copy(
                        title = response.title,
                        isCompleted = response.isCompleted,
                        position = response.position,
                        isSynced = true,
                        pendingAction = null
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating subtask id=${sub.id}: ${e.message}", e)
                hasFailures = true
            }
        }

        for (sub in subsToSync.filter { it.pendingAction == "delete" && it.id.toIntOrNull() != null }) {
            try {
                api.deleteSubtask(sub.id.toInt())
                subtaskDao.deleteById(sub.id)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting subtask id=${sub.id}: ${e.message}", e)
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

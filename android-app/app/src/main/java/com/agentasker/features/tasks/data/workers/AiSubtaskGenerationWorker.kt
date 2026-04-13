package com.agentasker.features.tasks.data.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.agentasker.core.ai.AiTaskService
import com.agentasker.core.hardware.HapticFeedbackManager
import com.agentasker.core.hardware.NotificationHelper
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AiSubtaskGenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val taskRepository: TaskRepository,
    private val aiTaskService: AiTaskService,
    private val notificationHelper: NotificationHelper,
    private val hapticFeedbackManager: HapticFeedbackManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notificationHelper.buildAiProgressNotification(
            title = "Generando subtareas con IA",
            body = "Analizando la tarea…"
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NotificationHelper.AI_SUBTASK_WORKER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NotificationHelper.AI_SUBTASK_WORKER_NOTIFICATION_ID,
                notification
            )
        }
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        Log.d(TAG, "doWork start taskId=$taskId")

        try { setForeground(getForegroundInfo()) } catch (e: Exception) {
            Log.w(TAG, "setForeground failed: ${e.message}")
        }

        val task = taskDao.getTaskByIdSync(taskId)
        if (task == null) {
            notifyResult(success = false, body = "No se encontró la tarea.")
            return Result.failure()
        }

        if (task.description.isBlank()) {
            notifyResult(
                success = false,
                body = "La tarea necesita una descripción para poder dividirla con IA."
            )
            return Result.failure()
        }

        return try {
            val titles = aiTaskService.splitIntoSubtasks(task.title, task.description)
            Log.d(TAG, "DeepSeek returned ${titles.size} subtasks")

            if (titles.isEmpty()) {
                notifyResult(success = false, body = "La IA no pudo generar subtareas.")
                return Result.failure()
            }

            taskRepository.replaceSubtasks(taskId, titles)

            notifyResult(
                success = true,
                body = "Se generaron ${titles.size} subtareas para '${task.title}'.",
                taskId = taskId
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error generando subtareas: ${e.message}", e)
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                notifyResult(
                    success = false,
                    body = "Error al generar subtareas: ${e.message?.take(80) ?: "desconocido"}"
                )
                Result.failure()
            }
        }
    }

    private fun notifyResult(success: Boolean, body: String, taskId: String? = null) {
        notificationHelper.showAiResultNotification(
            title = if (success) "Subtareas generadas" else "Error generando subtareas",
            body = body,
            success = success,
            screen = "tasks",
            taskId = taskId
        )
    }

    companion object {
        private const val TAG = "AiSubtaskWorker"
        private const val MAX_ATTEMPTS = 3
        const val KEY_TASK_ID = "taskId"
        const val WORK_NAME_PREFIX = "ai_split_"
    }
}

package com.agentasker.features.tasks.data.workers

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleSyncOnConnectivity() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                TaskSyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    fun scheduleImmediateSync() {
        val syncRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                TaskSyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    /**
     * Encola el worker que pide subtareas a DeepSeek para una task existente.
     * Expedited para arrancar rápido; si no hay cuota, cae a work normal.
     */
    fun scheduleAiSplit(taskId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AiSubtaskGenerationWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(AiSubtaskGenerationWorker.KEY_TASK_ID to taskId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("${AiSubtaskGenerationWorker.WORK_NAME_PREFIX}$taskId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "${AiSubtaskGenerationWorker.WORK_NAME_PREFIX}$taskId",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /**
     * Encola el worker que hace OCR + DeepSeek para una foto y crea
     * una task nueva con sus subtasks.
     */
    fun scheduleImageAnalysis(imageUri: Uri) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workName = "${AiImageAnalysisWorker.WORK_NAME_PREFIX}${System.currentTimeMillis()}"
        val request = OneTimeWorkRequestBuilder<AiImageAnalysisWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(AiImageAnalysisWorker.KEY_IMAGE_URI to imageUri.toString()))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(workName)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
    }
}

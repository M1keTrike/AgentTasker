package com.agentasker.features.tasks.data.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.agentasker.core.ai.AiTaskService
import com.agentasker.core.hardware.HapticFeedbackManager
import com.agentasker.core.hardware.NotificationHelper
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Worker que analiza una foto:
 *   1. Carga el Uri (local content://) como InputImage.
 *   2. Corre ML Kit Text Recognition on-device → ocrText.
 *   3. Envía el OCR a DeepSeek para que proponga una task completa
 *      (title, description, priority, subtasks).
 *   4. Crea la Task en Room/backend + las subtasks.
 *
 * Ejecuta como foreground service-type data-sync para sobrevivir al kill.
 *
 * Input data:
 *   KEY_IMAGE_URI -> String (Uri.toString())
 */
@HiltWorker
class AiImageAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val aiTaskService: AiTaskService,
    private val notificationHelper: NotificationHelper,
    private val hapticFeedbackManager: HapticFeedbackManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notificationHelper.buildAiProgressNotification(
            title = "Analizando imagen con IA",
            body = "Extrayendo texto y generando tarea…"
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NotificationHelper.AI_IMAGE_WORKER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NotificationHelper.AI_IMAGE_WORKER_NOTIFICATION_ID,
                notification
            )
        }
    }

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_IMAGE_URI) ?: return Result.failure()
        val uri = Uri.parse(uriString)
        Log.d(TAG, "doWork start uri=$uri")

        try { setForeground(getForegroundInfo()) } catch (e: Exception) {
            Log.w(TAG, "setForeground failed: ${e.message}")
        }

        return try {
            // 1. OCR on-device. ML Kit no sube nada a la red.
            val image = InputImage.fromFilePath(applicationContext, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image).await()
            val ocrText = visionText.text
            Log.d(TAG, "OCR extracted ${ocrText.length} chars")

            // 2. DeepSeek arma la task propuesta.
            val draft = aiTaskService.analyzeOcrToTask(ocrText)
            Log.d(TAG, "Draft title=${draft.title} subtasks=${draft.subtasks.size}")

            // 3. Crear la task en Room/backend. Offline-first.
            val created = taskRepository.createTask(
                title = draft.title,
                description = draft.description,
                priority = draft.priority
            )

            // 4. Subtasks (si las hay).
            if (draft.subtasks.isNotEmpty()) {
                taskRepository.createSubtasksBulk(created.id, draft.subtasks)
            }

            notificationHelper.showAiResultNotification(
                title = "Tarea creada",
                body = "'${draft.title}' con ${draft.subtasks.size} subtareas.",
                success = true,
                screen = "tasks",
                taskId = created.id
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error analizando imagen: ${e.message}", e)
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                notificationHelper.showAiResultNotification(
                    title = "Error analizando imagen",
                    body = e.message?.take(100) ?: "Error desconocido",
                    success = false
                )
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "AiImageWorker"
        private const val MAX_ATTEMPTS = 3
        const val KEY_IMAGE_URI = "imageUri"
        const val WORK_NAME_PREFIX = "ai_image_"
    }
}

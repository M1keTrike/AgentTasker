package com.agentasker.core.ai

import android.util.Log
import com.agentasker.core.ai.model.AiTaskDraft
import com.agentasker.core.ai.model.DeepSeekChatRequest
import com.agentasker.core.ai.model.DeepSeekMessage
import com.agentasker.core.ai.prompt.TaskDecompositionPrompt
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AiTaskService"

/**
 * Wrapper de alto nivel sobre DeepSeek. Encapsula el armado del prompt,
 * la llamada HTTP y el parseo del JSON devuelto. Los workers solo hablan
 * con este servicio y nunca tocan DeepSeekApi directamente.
 */
@Singleton
class AiTaskService @Inject constructor(
    private val api: DeepSeekApi,
    private val gson: Gson
) {

    /**
     * Dada una task, pide a DeepSeek una lista de subtareas. Devuelve la
     * lista o lanza si el parseo falla (el worker decide si retry o fail).
     */
    suspend fun splitIntoSubtasks(title: String, description: String): List<String> {
        val request = DeepSeekChatRequest(
            messages = listOf(
                DeepSeekMessage(role = "system", content = TaskDecompositionPrompt.SYSTEM),
                DeepSeekMessage(
                    role = "user",
                    content = TaskDecompositionPrompt.buildSubtaskSplit(title, description)
                )
            )
        )

        val response = api.chatCompletion(request)
        val raw = response.choices.firstOrNull()?.message?.content.orEmpty()
        Log.d(TAG, "splitIntoSubtasks raw response=$raw")

        return try {
            val parsed = gson.fromJson(raw, SplitResponse::class.java)
            parsed?.subtasks?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse DeepSeek response: $raw", e)
            throw IllegalStateException("Respuesta de IA no válida: $raw", e)
        }
    }

    /**
     * Dado texto extraído por OCR, pide a DeepSeek una task completa
     * (título, descripción, prioridad, subtasks).
     */
    suspend fun analyzeOcrToTask(ocrText: String): AiTaskDraft {
        val request = DeepSeekChatRequest(
            messages = listOf(
                DeepSeekMessage(role = "system", content = TaskDecompositionPrompt.SYSTEM),
                DeepSeekMessage(
                    role = "user",
                    content = TaskDecompositionPrompt.buildImageAnalysis(ocrText)
                )
            ),
            maxTokens = 1500
        )

        val response = api.chatCompletion(request)
        val raw = response.choices.firstOrNull()?.message?.content.orEmpty()
        Log.d(TAG, "analyzeOcrToTask raw response=$raw")

        return try {
            val parsed = gson.fromJson(raw, DraftResponse::class.java)
                ?: throw IllegalStateException("Respuesta vacía de IA")
            AiTaskDraft(
                title = parsed.title.takeIf { !it.isNullOrBlank() } ?: "Tarea generada por IA",
                description = parsed.description.orEmpty(),
                priority = normalizePriority(parsed.priority),
                subtasks = parsed.subtasks?.filter { it.isNotBlank() } ?: emptyList()
            )
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse DeepSeek response: $raw", e)
            throw IllegalStateException("Respuesta de IA no válida: $raw", e)
        }
    }

    private fun normalizePriority(value: String?): String = when (value?.lowercase()?.trim()) {
        "high", "alta" -> "high"
        "low", "baja" -> "low"
        else -> "medium"
    }

    private data class SplitResponse(
        val subtasks: List<String>?
    )

    private data class DraftResponse(
        val title: String?,
        val description: String?,
        val priority: String?,
        val subtasks: List<String>?
    )
}

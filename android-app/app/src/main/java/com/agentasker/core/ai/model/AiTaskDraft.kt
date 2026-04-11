package com.agentasker.core.ai.model

/**
 * Resultado estructurado del análisis de una imagen: lo que el usuario
 * verá como "task propuesta" antes de que se cree en Room/backend.
 */
data class AiTaskDraft(
    val title: String,
    val description: String,
    val priority: String = "medium",
    val subtasks: List<String> = emptyList()
)

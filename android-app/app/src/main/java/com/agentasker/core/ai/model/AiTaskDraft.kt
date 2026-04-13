package com.agentasker.core.ai.model

data class AiTaskDraft(
    val title: String,
    val description: String,
    val priority: String = "medium",
    val subtasks: List<String> = emptyList()
)

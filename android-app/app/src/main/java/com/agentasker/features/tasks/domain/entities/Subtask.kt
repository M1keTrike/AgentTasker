package com.agentasker.features.tasks.domain.entities

data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

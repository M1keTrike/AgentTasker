package com.agentasker.features.tasks.domain.entities

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)


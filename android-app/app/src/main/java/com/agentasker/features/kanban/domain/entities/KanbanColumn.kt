package com.agentasker.features.kanban.domain.entities

data class KanbanColumn(
    val id: String,
    val title: String,
    val statusKey: String,
    val position: Int,
    val color: String? = null
)

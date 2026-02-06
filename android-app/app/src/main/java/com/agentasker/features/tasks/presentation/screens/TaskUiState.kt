package com.agentasker.features.tasks.presentation.screens

import com.agentasker.features.tasks.domain.entities.Task

data class TaskUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val showDialog: Boolean = false,
    val taskToEdit: Task? = null,
    val formTitle: String = "",
    val formDescription: String = "",
    val formPriority: String = "medium"
)


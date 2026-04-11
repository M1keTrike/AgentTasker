package com.agentasker.features.kanban.presentation.screens

import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.entities.KanbanItem
import com.agentasker.features.tasks.presentation.service.TaskSyncService

data class KanbanUiState(
    val isLoading: Boolean = false,
    val columns: List<KanbanColumn> = emptyList(),
    val tasksByStatus: Map<String, List<KanbanItem>> = emptyMap(),
    val error: String? = null,
    val showColumnDialog: Boolean = false,
    val columnToEdit: KanbanColumn? = null,
    val formTitle: String = "",
    val formStatusKey: String = "",
    val formColor: String? = null,
    val syncState: TaskSyncService.SyncState = TaskSyncService.SyncState.Idle
)

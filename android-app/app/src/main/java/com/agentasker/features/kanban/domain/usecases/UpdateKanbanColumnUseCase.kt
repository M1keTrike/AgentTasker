package com.agentasker.features.kanban.domain.usecases

import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import javax.inject.Inject

class UpdateKanbanColumnUseCase @Inject constructor(
    private val repository: KanbanRepository
) {
    suspend operator fun invoke(
        id: String,
        title: String? = null,
        statusKey: String? = null,
        position: Int? = null,
        color: String? = null
    ): KanbanColumn = repository.updateColumn(id, title, statusKey, position, color)
}

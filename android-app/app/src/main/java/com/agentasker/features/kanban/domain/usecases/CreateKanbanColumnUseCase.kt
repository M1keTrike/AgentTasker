package com.agentasker.features.kanban.domain.usecases

import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import javax.inject.Inject

class CreateKanbanColumnUseCase @Inject constructor(
    private val repository: KanbanRepository
) {
    suspend operator fun invoke(
        title: String,
        statusKey: String,
        position: Int? = null,
        color: String? = null
    ): KanbanColumn = repository.createColumn(title, statusKey, position, color)
}

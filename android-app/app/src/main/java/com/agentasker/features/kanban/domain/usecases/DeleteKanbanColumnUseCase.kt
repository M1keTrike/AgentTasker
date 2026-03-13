package com.agentasker.features.kanban.domain.usecases

import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import javax.inject.Inject

class DeleteKanbanColumnUseCase @Inject constructor(
    private val repository: KanbanRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteColumn(id)
}

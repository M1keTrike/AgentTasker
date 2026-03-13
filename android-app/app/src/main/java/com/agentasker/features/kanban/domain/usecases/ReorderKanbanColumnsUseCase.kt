package com.agentasker.features.kanban.domain.usecases

import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import javax.inject.Inject

class ReorderKanbanColumnsUseCase @Inject constructor(
    private val repository: KanbanRepository
) {
    suspend operator fun invoke(columns: List<Pair<String, Int>>) =
        repository.reorderColumns(columns)
}

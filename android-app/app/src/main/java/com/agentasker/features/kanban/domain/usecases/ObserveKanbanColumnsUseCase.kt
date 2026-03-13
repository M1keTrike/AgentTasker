package com.agentasker.features.kanban.domain.usecases

import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveKanbanColumnsUseCase @Inject constructor(
    private val repository: KanbanRepository
) {
    operator fun invoke(): Flow<List<KanbanColumn>> = repository.observeColumns()
}

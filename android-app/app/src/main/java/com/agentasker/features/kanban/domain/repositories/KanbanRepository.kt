package com.agentasker.features.kanban.domain.repositories

import com.agentasker.features.kanban.domain.entities.KanbanColumn
import kotlinx.coroutines.flow.Flow

interface KanbanRepository {
    fun observeColumns(): Flow<List<KanbanColumn>>
    suspend fun refreshColumns()
    suspend fun createColumn(title: String, statusKey: String, position: Int? = null, color: String? = null): KanbanColumn
    suspend fun updateColumn(id: String, title: String? = null, statusKey: String? = null, position: Int? = null, color: String? = null): KanbanColumn
    suspend fun deleteColumn(id: String)
    suspend fun reorderColumns(columns: List<Pair<String, Int>>)
}

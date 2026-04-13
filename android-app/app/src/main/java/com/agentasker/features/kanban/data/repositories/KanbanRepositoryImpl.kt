package com.agentasker.features.kanban.data.repositories

import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.core.network.NetworkMonitor
import com.agentasker.features.kanban.data.datasources.local.dao.KanbanColumnDao
import com.agentasker.features.kanban.data.datasources.local.entities.KanbanColumnEntity
import com.agentasker.features.kanban.data.datasources.local.mapper.toDomain
import com.agentasker.features.kanban.data.datasources.local.mapper.toEntities
import com.agentasker.features.kanban.data.datasources.local.mapper.toEntity
import com.agentasker.features.kanban.data.datasources.remote.model.CreateKanbanColumnRequest
import com.agentasker.features.kanban.data.datasources.remote.model.ReorderItem
import com.agentasker.features.kanban.data.datasources.remote.model.ReorderKanbanColumnsRequest
import com.agentasker.features.kanban.data.datasources.remote.model.UpdateKanbanColumnRequest
import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class KanbanRepositoryImpl @Inject constructor(
    private val api: AgentTaskerApi,
    private val columnDao: KanbanColumnDao,
    private val networkMonitor: NetworkMonitor
) : KanbanRepository {

    private suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    override fun observeColumns(): Flow<List<KanbanColumn>> {
        return columnDao.getAllColumns().map { entities -> entities.toDomain() }
    }

    override suspend fun refreshColumns() {
        if (!isOnline()) return
        try {
            val remoteColumns = api.getKanbanColumns()
            columnDao.upsertColumns(remoteColumns.toEntities(isSynced = true))
        } catch (_: Exception) { }
    }

    override suspend fun createColumn(
        title: String,
        statusKey: String,
        position: Int?,
        color: String?
    ): KanbanColumn {
        val request = CreateKanbanColumnRequest(
            title = title,
            statusKey = statusKey,
            position = position,
            color = color
        )
        if (isOnline()) {
            try {
                val response = api.createKanbanColumn(request)
                val entity = response.toEntity(isSynced = true)
                columnDao.upsertColumn(entity)
                return entity.toDomain()
            } catch (_: Exception) { }
        }
        val localEntity = KanbanColumnEntity(
            id = "local_${System.currentTimeMillis()}",
            title = title,
            statusKey = statusKey,
            position = position ?: 0,
            color = color,
            isSynced = false,
            pendingAction = "create"
        )
        columnDao.upsertColumn(localEntity)
        return localEntity.toDomain()
    }

    override suspend fun updateColumn(
        id: String,
        title: String?,
        statusKey: String?,
        position: Int?,
        color: String?
    ): KanbanColumn {
        val request = UpdateKanbanColumnRequest(
            title = title,
            statusKey = statusKey,
            position = position,
            color = color
        )
        if (isOnline()) {
            try {
                val response = api.updateKanbanColumn(id.toInt(), request)
                val entity = response.toEntity(isSynced = true)
                columnDao.upsertColumn(entity)
                return entity.toDomain()
            } catch (_: Exception) { }
        }
        val current = columnDao.getColumnByIdSync(id)
            ?: throw Exception("Columna no encontrada")
        val action = if (current.pendingAction == "create") "create" else "update"
        val updated = current.copy(
            title = title ?: current.title,
            statusKey = statusKey ?: current.statusKey,
            position = position ?: current.position,
            color = color ?: current.color,
            isSynced = false,
            pendingAction = action
        )
        columnDao.upsertColumn(updated)
        return updated.toDomain()
    }

    override suspend fun deleteColumn(id: String) {
        val existing = columnDao.getColumnByIdSync(id)

        if (existing?.pendingAction == "create") {
            columnDao.deleteColumnById(id)
            return
        }

        if (isOnline()) {
            try {
                api.deleteKanbanColumn(id.toInt())
                columnDao.deleteColumnById(id)
                return
            } catch (_: Exception) { }
        }

        if (existing != null) {
            columnDao.upsertColumn(existing.copy(isSynced = false, pendingAction = "delete"))
        } else {
            columnDao.deleteColumnById(id)
        }
    }

    override suspend fun reorderColumns(columns: List<Pair<String, Int>>) {
        for ((id, position) in columns) {
            val existing = columnDao.getColumnByIdSync(id) ?: continue
            columnDao.upsertColumn(existing.copy(position = position))
        }

        if (isOnline()) {
            try {
                val request = ReorderKanbanColumnsRequest(
                    columns = columns.map { (id, position) ->
                        ReorderItem(id = id.toInt(), position = position)
                    }
                )
                api.reorderKanbanColumns(request)
            } catch (_: Exception) { }
        }
    }
}

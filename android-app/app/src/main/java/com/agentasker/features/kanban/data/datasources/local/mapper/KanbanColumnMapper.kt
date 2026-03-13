package com.agentasker.features.kanban.data.datasources.local.mapper

import com.agentasker.features.kanban.data.datasources.local.entities.KanbanColumnEntity
import com.agentasker.features.kanban.data.datasources.remote.model.KanbanColumnDTO
import com.agentasker.features.kanban.domain.entities.KanbanColumn

fun KanbanColumnEntity.toDomain(): KanbanColumn {
    return KanbanColumn(
        id = id,
        title = title,
        statusKey = statusKey,
        position = position,
        color = color
    )
}

fun KanbanColumnDTO.toEntity(isSynced: Boolean = true): KanbanColumnEntity {
    return KanbanColumnEntity(
        id = id.toString(),
        title = title,
        statusKey = statusKey,
        position = position,
        color = color,
        isSynced = isSynced
    )
}

fun List<KanbanColumnEntity>.toDomain(): List<KanbanColumn> = map { it.toDomain() }

fun List<KanbanColumnDTO>.toEntities(isSynced: Boolean = true): List<KanbanColumnEntity> =
    map { it.toEntity(isSynced) }

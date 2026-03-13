package com.agentasker.features.tasks.data.datasources.local.mapper

import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity
import com.agentasker.features.tasks.data.datasources.remote.model.TaskDTO
import com.agentasker.features.tasks.domain.entities.Task

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Task.toEntity(isSynced: Boolean = true): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = isSynced
    )
}

fun TaskDTO.toEntity(isSynced: Boolean = true): TaskEntity {
    return TaskEntity(
        id = id.toString(),
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = isSynced
    )
}

fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }

fun List<TaskDTO>.toEntities(isSynced: Boolean = true): List<TaskEntity> = map { it.toEntity(isSynced) }

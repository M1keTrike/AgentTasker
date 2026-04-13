package com.agentasker.features.tasks.data.datasources.local.mapper

import com.agentasker.features.tasks.data.datasources.local.entities.SubtaskEntity
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity
import com.agentasker.features.tasks.data.datasources.remote.model.SubtaskDTO
import com.agentasker.features.tasks.data.datasources.remote.model.TaskDTO
import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.entities.TaskSource

fun TaskEntity.toDomain(subtasks: List<Subtask> = emptyList()): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        source = source,
        externalId = externalId,
        courseName = courseName,
        externalLink = externalLink,
        isArchived = isArchived,
        subtasks = subtasks,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Task.toEntity(isSynced: Boolean = true, pendingAction: String? = null): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        source = source,
        externalId = externalId,
        courseName = courseName,
        externalLink = externalLink,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = isSynced,
        pendingAction = pendingAction
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
        source = source ?: TaskSource.LOCAL,
        externalId = externalId,
        courseName = courseName,
        externalLink = externalLink,
        isArchived = isArchived ?: false,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = isSynced
    )
}

fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }

fun List<TaskDTO>.toEntities(isSynced: Boolean = true): List<TaskEntity> = map { it.toEntity(isSynced) }

fun SubtaskEntity.toDomain(): Subtask = Subtask(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Subtask.toEntity(isSynced: Boolean = true, pendingAction: String? = null): SubtaskEntity = SubtaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced,
    pendingAction = pendingAction
)

fun SubtaskDTO.toEntity(isSynced: Boolean = true): SubtaskEntity = SubtaskEntity(
    id = id.toString(),
    taskId = taskId.toString(),
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced
)

fun List<SubtaskEntity>.subtasksToDomain(): List<Subtask> = map { it.toDomain() }
fun List<SubtaskDTO>.subtasksToEntities(isSynced: Boolean = true): List<SubtaskEntity> =
    map { it.toEntity(isSynced) }

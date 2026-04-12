package com.agentasker.features.tasks.data.datasources.remote.mapper

import com.agentasker.features.tasks.data.datasources.remote.model.SubtaskDTO
import com.agentasker.features.tasks.data.datasources.remote.model.TaskDTO
import com.agentasker.features.tasks.data.datasources.remote.model.TaskResponse
import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.entities.TaskSource

fun SubtaskDTO.toDomain(): Subtask = Subtask(
    id = id.toString(),
    taskId = taskId.toString(),
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskDTO.toDomain(): Task {
    return Task(
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
        subtasks = subtasks?.map { it.toDomain() } ?: emptyList(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaskResponse.toDomain(): List<Task> {
    return this.map { it.toDomain() }
}

package com.agentasker.features.tasks.data.datasources.remote.mapper

import com.agentasker.features.tasks.data.datasources.remote.model.TaskDTO
import com.agentasker.features.tasks.data.datasources.remote.model.TaskResponse
import com.agentasker.features.tasks.domain.entities.Task

fun TaskDTO.toDomain(): Task {
    return Task(
        id = id.toString(),
        title = title,
        description = description,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaskResponse.toDomain(): List<Task> {
    return this.map { it.toDomain() }
}


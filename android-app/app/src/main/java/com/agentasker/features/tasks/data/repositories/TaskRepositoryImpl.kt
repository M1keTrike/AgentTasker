package com.agentasker.features.tasks.data.repositories

import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.tasks.data.datasources.remote.mapper.toDomain
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository

class TaskRepositoryImpl(
    private val api: AgentTaskerApi
) : TaskRepository {

    override suspend fun getTasks(): List<Task> {
        val response = api.getTasks()
        return response.toDomain()
    }

    override suspend fun getTaskById(id: String): Task {
        val response = api.getTaskById(id.toInt())
        return response.toDomain()
    }

    override suspend fun createTask(title: String, description: String, priority: String): Task {
        val request = CreateTaskRequest(
            title = title,
            description = description,
            priority = priority
        )
        val response = api.createTask(request)
        return response.toDomain()
    }

    override suspend fun updateTask(
        id: String,
        title: String?,
        description: String?,
        priority: String?
    ): Task {
        val request = UpdateTaskRequest(
            title = title,
            description = description,
            priority = priority
        )
        val response = api.updateTask(id.toInt(), request)
        return response.toDomain()
    }

    override suspend fun deleteTask(id: String) {
        api.deleteTask(id.toInt())
    }
}


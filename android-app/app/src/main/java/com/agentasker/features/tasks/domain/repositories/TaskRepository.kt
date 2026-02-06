package com.agentasker.features.tasks.domain.repositories

import com.agentasker.features.tasks.domain.entities.Task

interface TaskRepository {
    suspend fun getTasks(): List<Task>
    suspend fun getTaskById(id: String): Task
    suspend fun createTask(title: String, description: String, priority: String): Task
    suspend fun updateTask(id: String, title: String?, description: String?, priority: String?): Task
    suspend fun deleteTask(id: String)
}


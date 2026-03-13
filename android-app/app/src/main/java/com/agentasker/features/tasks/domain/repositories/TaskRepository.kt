package com.agentasker.features.tasks.domain.repositories

import com.agentasker.features.tasks.domain.entities.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun refreshTasks()
    suspend fun getTasks(): List<Task>
    suspend fun getTaskById(id: String): Task
    suspend fun createTask(title: String, description: String, priority: String): Task
    suspend fun updateTask(id: String, title: String?, description: String?, priority: String?): Task
    suspend fun deleteTask(id: String)
}

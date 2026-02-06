package com.agentasker.features.tasks.domain.usecases

import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        id: String,
        title: String?,
        description: String?,
        priority: String?
    ): Result<Task> {
        return try {
            val task = taskRepository.updateTask(id, title, description, priority)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


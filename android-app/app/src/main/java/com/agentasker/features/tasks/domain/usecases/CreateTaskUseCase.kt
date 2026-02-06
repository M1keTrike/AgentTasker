package com.agentasker.features.tasks.domain.usecases

import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository

class CreateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(title: String, description: String, priority: String): Result<Task> {
        return try {
            val task = taskRepository.createTask(title, description, priority)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

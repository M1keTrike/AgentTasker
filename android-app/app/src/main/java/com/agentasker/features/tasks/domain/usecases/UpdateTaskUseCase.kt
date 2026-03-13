package com.agentasker.features.tasks.domain.usecases

import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        id: String,
        title: String?,
        description: String?,
        priority: String?,
        status: String? = null,
        dueDate: String? = null
    ): Result<Task> {
        return try {
            val task = taskRepository.updateTask(id, title, description, priority, status, dueDate)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


package com.agentasker.features.tasks.domain.usecases

import com.agentasker.features.tasks.domain.repositories.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            taskRepository.deleteTask(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


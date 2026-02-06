package com.agentasker.features.tasks.domain.usecases

import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository

class GetTasksUseCase(
    private val repository: TaskRepository
) {

    suspend operator fun invoke(): Result<List<Task>> {
        return try {
            val tasks = repository.getTasks()

            val filteredTasks = tasks.filter { it.title.isNotBlank() }

            if (filteredTasks.isEmpty()) {
                Result.failure(Exception("No se encontraron tareas válidas"))
            } else {
                Result.success(filteredTasks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


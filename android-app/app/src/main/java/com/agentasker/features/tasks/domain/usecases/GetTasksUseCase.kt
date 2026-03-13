package com.agentasker.features.tasks.domain.usecases

import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {

    operator fun invoke(): Flow<List<Task>> {
        return repository.observeTasks().map { tasks ->
            tasks.filter { it.title.isNotBlank() }
        }
    }

    suspend fun refresh() {
        repository.refreshTasks()
    }
}

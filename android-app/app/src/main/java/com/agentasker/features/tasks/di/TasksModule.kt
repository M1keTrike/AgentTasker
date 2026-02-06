package com.agentasker.features.tasks.di

import com.agentasker.core.di.AppContainer
import com.agentasker.features.tasks.domain.usecases.CreateTaskUseCase
import com.agentasker.features.tasks.domain.usecases.DeleteTaskUseCase
import com.agentasker.features.tasks.domain.usecases.GetTasksUseCase
import com.agentasker.features.tasks.domain.usecases.UpdateTaskUseCase
import com.agentasker.features.tasks.presentation.viewmodel.TaskViewModelFactory

class TasksModule(
    private val appContainer: AppContainer
) {

    private fun provideGetTasksUseCase(): GetTasksUseCase {
        return GetTasksUseCase(appContainer.taskRepository)
    }

    private fun provideCreateTaskUseCase(): CreateTaskUseCase {
        return CreateTaskUseCase(appContainer.taskRepository)
    }

    private fun provideUpdateTaskUseCase(): UpdateTaskUseCase {
        return UpdateTaskUseCase(appContainer.taskRepository)
    }

    private fun provideDeleteTaskUseCase(): DeleteTaskUseCase {
        return DeleteTaskUseCase(appContainer.taskRepository)
    }

    fun provideTaskViewModelFactory(): TaskViewModelFactory {
        return TaskViewModelFactory(
            getTasksUseCase = provideGetTasksUseCase(),
            createTaskUseCase = provideCreateTaskUseCase(),
            updateTaskUseCase = provideUpdateTaskUseCase(),
            deleteTaskUseCase = provideDeleteTaskUseCase()
        )
    }
}


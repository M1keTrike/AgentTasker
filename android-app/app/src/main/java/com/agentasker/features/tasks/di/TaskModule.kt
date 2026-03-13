package com.agentasker.features.tasks.di

import com.agentasker.features.tasks.data.repositories.TaskReminderRepositoryImpl
import com.agentasker.features.tasks.data.repositories.TaskRepositoryImpl
import com.agentasker.features.tasks.domain.repositories.TaskReminderRepository
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TaskModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTaskReminderRepository(impl: TaskReminderRepositoryImpl): TaskReminderRepository
}

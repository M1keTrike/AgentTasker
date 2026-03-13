package com.agentasker.features.kanban.di

import com.agentasker.features.kanban.data.repositories.KanbanRepositoryImpl
import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KanbanModule {

    @Binds
    @Singleton
    abstract fun bindKanbanRepository(impl: KanbanRepositoryImpl): KanbanRepository
}

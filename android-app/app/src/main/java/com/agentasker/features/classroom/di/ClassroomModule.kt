package com.agentasker.features.classroom.di

import com.agentasker.features.classroom.data.repositories.ClassroomRepositoryImpl
import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassroomModule {

    @Binds
    @Singleton
    abstract fun bindClassroomRepository(impl: ClassroomRepositoryImpl): ClassroomRepository
}

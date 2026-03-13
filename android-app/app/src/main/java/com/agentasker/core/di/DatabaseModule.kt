package com.agentasker.core.di

import android.content.Context
import androidx.room.Room
import com.agentasker.core.database.AppDatabase
import com.agentasker.features.tasks.data.datasources.local.dao.TaskReminderDao
import com.agentasker.features.classroom.data.datasources.local.dao.ClassroomTaskDao
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "agentasker_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideClassroomTaskDao(database: AppDatabase): ClassroomTaskDao {
        return database.classroomTaskDao()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideTaskReminderDao(database: AppDatabase): TaskReminderDao {
        return database.taskReminderDao()
    }
}

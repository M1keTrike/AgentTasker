package com.agentasker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agentasker.features.classroom.data.datasources.local.dao.ClassroomTaskDao
import com.agentasker.features.classroom.data.datasources.local.entities.ClassroomTaskEntity
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity

@Database(
    entities = [ClassroomTaskEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classroomTaskDao(): ClassroomTaskDao
    abstract fun taskDao(): TaskDao
}

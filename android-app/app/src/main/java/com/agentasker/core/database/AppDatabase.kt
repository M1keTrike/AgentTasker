package com.agentasker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agentasker.features.classroom.data.datasources.local.dao.ClassroomTaskDao
import com.agentasker.features.classroom.data.datasources.local.entities.ClassroomTaskEntity

@Database(
    entities = [ClassroomTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classroomTaskDao(): ClassroomTaskDao
}

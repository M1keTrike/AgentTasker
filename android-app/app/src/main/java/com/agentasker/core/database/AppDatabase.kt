package com.agentasker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agentasker.features.tasks.data.datasources.local.dao.TaskReminderDao
import com.agentasker.features.tasks.data.datasources.local.entities.TaskReminderEntity
import com.agentasker.features.classroom.data.datasources.local.dao.ClassroomTaskDao
import com.agentasker.features.classroom.data.datasources.local.entities.ClassroomTaskEntity
import com.agentasker.features.kanban.data.datasources.local.dao.KanbanColumnDao
import com.agentasker.features.kanban.data.datasources.local.entities.KanbanColumnEntity
import com.agentasker.features.tasks.data.datasources.local.dao.SubtaskDao
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.entities.SubtaskEntity
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity

@Database(
    entities = [
        ClassroomTaskEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        TaskReminderEntity::class,
        KanbanColumnEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classroomTaskDao(): ClassroomTaskDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun taskReminderDao(): TaskReminderDao
    abstract fun kanbanColumnDao(): KanbanColumnDao
}

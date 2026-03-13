package com.agentasker.features.classroom.data.datasources.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.agentasker.features.classroom.data.datasources.local.entities.ClassroomTaskEntity

@Dao
interface ClassroomTaskDao {

    @Query("SELECT * FROM classroom_tasks ORDER BY dueDate ASC")
    suspend fun getAllTasks(): List<ClassroomTaskEntity>

    @Query("SELECT * FROM classroom_tasks WHERE courseId = :courseId ORDER BY dueDate ASC")
    suspend fun getTasksByCourse(courseId: String): List<ClassroomTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ClassroomTaskEntity>)

    @Query("DELETE FROM classroom_tasks")
    suspend fun clearAll()

    @Transaction
    suspend fun clearAndInsertAll(tasks: List<ClassroomTaskEntity>) {
        clearAll()
        insertTasks(tasks)
    }
}

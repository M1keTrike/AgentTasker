package com.agentasker.features.tasks.data.datasources.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.agentasker.features.tasks.data.datasources.local.entities.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Query("SELECT * FROM subtasks WHERE pendingAction IS NULL OR pendingAction != 'delete'")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId AND (pendingAction IS NULL OR pendingAction != 'delete') ORDER BY position ASC")
    fun observeByTaskId(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC")
    suspend fun getByTaskId(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: String): SubtaskEntity?

    @Query("SELECT * FROM subtasks WHERE pendingAction IS NOT NULL")
    suspend fun getPending(): List<SubtaskEntity>

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    suspend fun countByTaskId(taskId: String): Int

    @Upsert
    suspend fun upsert(subtask: SubtaskEntity)

    @Upsert
    suspend fun upsertAll(subtasks: List<SubtaskEntity>)

    @Delete
    suspend fun delete(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)
}

package com.agentasker.features.tasks.data.datasources.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND (pendingAction IS NULL OR pendingAction != 'delete') ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 1 AND (pendingAction IS NULL OR pendingAction != 'delete') ORDER BY updatedAt DESC")
    fun getArchivedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdSync(id: String): TaskEntity?

    /**
     * Busca una task por su externalId ignorando el filtro de archivadas.
     * Usado por el sync de Classroom para idempotencia: si una task ya
     * existe (incluso archivada), la reconocemos y preservamos los flags
     * locales (isArchived, status) en vez de re-importarla como nueva.
     */
    @Query("SELECT * FROM tasks WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternalId(externalId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE priority = :priority")
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE pendingAction IS NOT NULL")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

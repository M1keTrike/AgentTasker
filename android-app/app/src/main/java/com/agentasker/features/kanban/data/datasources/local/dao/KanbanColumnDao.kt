package com.agentasker.features.kanban.data.datasources.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agentasker.features.kanban.data.datasources.local.entities.KanbanColumnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KanbanColumnDao {

    @Query("SELECT * FROM kanban_columns WHERE pendingAction IS NULL OR pendingAction != 'delete' ORDER BY position ASC")
    fun getAllColumns(): Flow<List<KanbanColumnEntity>>

    @Query("SELECT * FROM kanban_columns WHERE id = :id LIMIT 1")
    suspend fun getColumnByIdSync(id: String): KanbanColumnEntity?

    @Query("SELECT * FROM kanban_columns WHERE pendingAction IS NOT NULL")
    suspend fun getPendingColumns(): List<KanbanColumnEntity>

    @Upsert
    suspend fun upsertColumn(column: KanbanColumnEntity)

    @Upsert
    suspend fun upsertColumns(columns: List<KanbanColumnEntity>)

    @Query("DELETE FROM kanban_columns WHERE id = :id")
    suspend fun deleteColumnById(id: String)

    @Query("DELETE FROM kanban_columns")
    suspend fun clearAll()
}

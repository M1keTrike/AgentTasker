package com.agentasker.features.kanban.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kanban_columns")
data class KanbanColumnEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val statusKey: String,
    val position: Int = 0,
    val color: String? = null,
    val isSynced: Boolean = true,
    val pendingAction: String? = null // "create", "update", "delete", or null (synced)
)

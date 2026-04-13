package com.agentasker.features.tasks.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class SubtaskEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val isSynced: Boolean = true,
    val pendingAction: String? = null
)

package com.agentasker.features.tasks.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val isSynced: Boolean = true
)

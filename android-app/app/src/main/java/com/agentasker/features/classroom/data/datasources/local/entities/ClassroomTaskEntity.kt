package com.agentasker.features.classroom.data.datasources.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classroom_tasks")
data class ClassroomTaskEntity(
    @PrimaryKey
    val id: String,
    val courseId: String,
    val courseName: String,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val submissionState: String,
    val alternateLink: String?,
    val maxPoints: Double?
)

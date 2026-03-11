package com.agentasker.features.classroom.domain.entities

import java.time.LocalDateTime

data class ClassroomTask(
    val id: String,
    val courseId: String,
    val courseName: String,
    val title: String,
    val description: String?,
    val dueDate: LocalDateTime?,
    val submissionState: SubmissionState,
    val alternateLink: String?,
    val maxPoints: Double?
)

enum class SubmissionState {
    NEW,
    CREATED,
    TURNED_IN,
    RETURNED,
    RECLAIMED_BY_STUDENT;

    companion object {
        fun fromString(value: String): SubmissionState {
            return entries.find { it.name == value } ?: NEW
        }
    }
}

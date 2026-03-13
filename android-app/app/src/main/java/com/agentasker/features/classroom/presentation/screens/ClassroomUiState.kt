package com.agentasker.features.classroom.presentation.screens

import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.entities.ClassroomTask

data class ClassroomUiState(
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val courses: List<ClassroomCourse> = emptyList(),
    val tasks: List<ClassroomTask> = emptyList(),
    val selectedCourseId: String? = null,
    val error: String? = null,
    val needsReauth: Boolean = false
)

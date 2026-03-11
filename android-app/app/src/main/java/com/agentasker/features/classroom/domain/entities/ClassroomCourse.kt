package com.agentasker.features.classroom.domain.entities

data class ClassroomCourse(
    val id: String,
    val name: String,
    val section: String?,
    val alternateLink: String
)

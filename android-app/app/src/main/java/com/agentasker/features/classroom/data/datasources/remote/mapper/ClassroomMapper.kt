package com.agentasker.features.classroom.data.datasources.remote.mapper

import com.agentasker.features.classroom.data.datasources.remote.model.ClassroomCourseDTO
import com.agentasker.features.classroom.data.datasources.remote.model.ClassroomTaskDTO
import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.classroom.domain.entities.SubmissionState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun ClassroomCourseDTO.toDomain(): ClassroomCourse = ClassroomCourse(
    id = id,
    name = name,
    section = section,
    alternateLink = alternateLink
)

fun ClassroomTaskDTO.toDomain(): ClassroomTask = ClassroomTask(
    id = id,
    courseId = courseId,
    courseName = courseName,
    title = title,
    description = description,
    dueDate = dueDate?.let { parseDate(it) },
    submissionState = SubmissionState.fromString(submissionState),
    alternateLink = alternateLink,
    maxPoints = maxPoints
)

private fun parseDate(dateString: String): LocalDateTime? {
    return try {
        LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        null
    }
}

fun List<ClassroomCourseDTO>.toDomainCourses(): List<ClassroomCourse> = map { it.toDomain() }

fun List<ClassroomTaskDTO>.toDomainTasks(): List<ClassroomTask> = map { it.toDomain() }

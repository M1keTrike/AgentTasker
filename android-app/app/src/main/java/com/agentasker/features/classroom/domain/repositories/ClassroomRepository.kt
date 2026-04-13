package com.agentasker.features.classroom.domain.repositories

import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.entities.ClassroomTask

interface ClassroomRepository {
    suspend fun connectClassroom(idToken: String, authorizationCode: String, codeVerifier: String? = null): Result<Unit>
    suspend fun getCourses(): Result<List<ClassroomCourse>>
    suspend fun getTasksByCourse(courseId: String): Result<List<ClassroomTask>>
    suspend fun getAllTasks(): Result<List<ClassroomTask>>
    suspend fun isClassroomConnected(): Result<Boolean>

    suspend fun syncClassroomTasksToLocal(): Result<Int>

    suspend fun syncClassroomTasksByCourses(courseIds: List<String>): Result<Int>
}

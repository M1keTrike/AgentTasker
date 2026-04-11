package com.agentasker.features.classroom.domain.repositories

import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.entities.ClassroomTask

interface ClassroomRepository {
    suspend fun connectClassroom(idToken: String, authorizationCode: String, codeVerifier: String? = null): Result<Unit>
    suspend fun getCourses(): Result<List<ClassroomCourse>>
    suspend fun getTasksByCourse(courseId: String): Result<List<ClassroomTask>>
    suspend fun getAllTasks(): Result<List<ClassroomTask>>
    suspend fun isClassroomConnected(): Result<Boolean>

    /**
     * Trae las tasks pendientes de Classroom (submissionState NEW o CREATED,
     * descartando TURNED_IN / RETURNED / RECLAIMED_BY_STUDENT) y las mapea al
     * modelo local `Task` con `source = "classroom"`. Cada task se upsertea
     * usando `externalId` como clave de idempotencia.
     *
     * Devuelve el número de tasks importadas.
     */
    suspend fun syncClassroomTasksToLocal(): Result<Int>
}

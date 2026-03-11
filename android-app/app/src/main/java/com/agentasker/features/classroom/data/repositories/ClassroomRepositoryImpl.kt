package com.agentasker.features.classroom.data.repositories

import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.classroom.data.datasources.local.dao.ClassroomTaskDao
import com.agentasker.features.classroom.data.datasources.local.entities.ClassroomTaskEntity
import com.agentasker.features.classroom.data.datasources.remote.mapper.toDomain
import com.agentasker.features.classroom.data.datasources.remote.mapper.toDomainCourses
import com.agentasker.features.classroom.data.datasources.remote.mapper.toDomainTasks
import com.agentasker.features.classroom.data.datasources.remote.model.ClassroomConnectRequestDTO
import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.classroom.domain.entities.SubmissionState
import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ClassroomRepositoryImpl @Inject constructor(
    private val api: AgentTaskerApi,
    private val classroomTaskDao: ClassroomTaskDao
) : ClassroomRepository {

    override suspend fun connectClassroom(
        idToken: String,
        authorizationCode: String
    ): Result<Unit> {
        return try {
            api.connectClassroom(
                ClassroomConnectRequestDTO(
                    idToken = idToken,
                    authorizationCode = authorizationCode
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCourses(): Result<List<ClassroomCourse>> {
        return try {
            val courses = api.getClassroomCourses()
            Result.success(courses.toDomainCourses())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTasksByCourse(courseId: String): Result<List<ClassroomTask>> {
        return try {
            val tasks = api.getClassroomTasksByCourse(courseId)
            val domainTasks = tasks.toDomainTasks()

            // Cache tasks locally
            val entities = domainTasks.map { it.toEntity() }
            classroomTaskDao.insertTasks(entities)

            Result.success(domainTasks)
        } catch (e: Exception) {
            // Fallback to cached data
            val cached = classroomTaskDao.getTasksByCourse(courseId)
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAllTasks(): Result<List<ClassroomTask>> {
        return try {
            val tasks = api.getAllClassroomTasks()
            val domainTasks = tasks.toDomainTasks()

            // Cache tasks locally
            val entities = domainTasks.map { it.toEntity() }
            classroomTaskDao.clearAndInsertAll(entities)

            Result.success(domainTasks)
        } catch (e: Exception) {
            // Fallback to cached data
            val cached = classroomTaskDao.getAllTasks()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun isClassroomConnected(): Result<Boolean> {
        return try {
            val status = api.getClassroomStatus()
            Result.success(status.connected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ClassroomTask.toEntity(): ClassroomTaskEntity = ClassroomTaskEntity(
        id = id,
        courseId = courseId,
        courseName = courseName,
        title = title,
        description = description,
        dueDate = dueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        submissionState = submissionState.name,
        alternateLink = alternateLink,
        maxPoints = maxPoints
    )

    private fun ClassroomTaskEntity.toDomain(): ClassroomTask = ClassroomTask(
        id = id,
        courseId = courseId,
        courseName = courseName,
        title = title,
        description = description,
        dueDate = dueDate?.let {
            try {
                LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                null
            }
        },
        submissionState = SubmissionState.fromString(submissionState),
        alternateLink = alternateLink,
        maxPoints = maxPoints
    )
}

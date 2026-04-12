package com.agentasker.features.classroom.data.repositories

import android.util.Log
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
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.entities.TaskSource
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "ClassroomRepo"

class ClassroomRepositoryImpl @Inject constructor(
    private val api: AgentTaskerApi,
    private val classroomTaskDao: ClassroomTaskDao,
    private val taskRepository: TaskRepository
) : ClassroomRepository {

    companion object {
        private const val REDIRECT_URI = "https://agentaskerapi.alphahills.site/auth/classroom/callback"

        // Estados que cuentan como "tarea pendiente para el alumno".
        private val PENDING_STATES = setOf(
            SubmissionState.NEW,
            SubmissionState.CREATED,
            SubmissionState.RECLAIMED_BY_STUDENT
        )
    }

    override suspend fun connectClassroom(
        idToken: String,
        authorizationCode: String,
        codeVerifier: String?
    ): Result<Unit> {
        return try {
            api.connectClassroom(
                ClassroomConnectRequestDTO(
                    idToken = idToken,
                    authorizationCode = authorizationCode,
                    redirectUri = REDIRECT_URI,
                    codeVerifier = codeVerifier
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

            val entities = domainTasks.map { it.toEntity() }
            classroomTaskDao.insertTasks(entities)

            Result.success(domainTasks)
        } catch (e: Exception) {
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

            val entities = domainTasks.map { it.toEntity() }
            classroomTaskDao.clearAndInsertAll(entities)

            Result.success(domainTasks)
        } catch (e: Exception) {
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

    override suspend fun syncClassroomTasksToLocal(): Result<Int> {
        return try {
            val remote = api.getAllClassroomTasks()
            val pending = remote.toDomainTasks().filter { it.submissionState in PENDING_STATES }
            Log.d(TAG, "syncAll: ${remote.size} remote -> ${pending.size} pending to import")

            pending.forEach { ct ->
                val task = ct.toLocalTask()
                taskRepository.upsertImportedTask(task)
            }

            Result.success(pending.size)
        } catch (e: Exception) {
            Log.e(TAG, "syncClassroomTasksToLocal failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncClassroomTasksByCourses(courseIds: List<String>): Result<Int> {
        if (courseIds.isEmpty()) return Result.success(0)
        return try {
            var total = 0
            for (courseId in courseIds) {
                val remote = api.getClassroomTasksByCourse(courseId)
                val pending = remote.toDomainTasks().filter { it.submissionState in PENDING_STATES }
                Log.d(TAG, "syncByCourse($courseId): ${remote.size} remote -> ${pending.size} pending")

                pending.forEach { ct ->
                    taskRepository.upsertImportedTask(ct.toLocalTask())
                }
                total += pending.size
            }
            Result.success(total)
        } catch (e: Exception) {
            Log.e(TAG, "syncClassroomTasksByCourses failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun ClassroomTask.toLocalTask(): Task {
        // Classroom no expone prioridad. Derivamos una prioridad por heurística
        // sencilla: si la due date está a menos de 48h, "high"; menos de 7 días,
        // "medium"; resto, "low". Si no hay dueDate, default medium.
        val priority = dueDate?.let {
            val hoursUntil = java.time.Duration.between(LocalDateTime.now(), it).toHours()
            when {
                hoursUntil <= 0 -> "high"
                hoursUntil <= 48 -> "high"
                hoursUntil <= 24 * 7 -> "medium"
                else -> "low"
            }
        } ?: "medium"

        return Task(
            id = "cls_$id", // placeholder local; upsertImportedTask re-usa existing si hay match por externalId
            title = title,
            description = description.orEmpty(),
            priority = priority,
            status = "pending",
            dueDate = dueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            source = TaskSource.CLASSROOM,
            externalId = id,
            courseName = courseName,
            externalLink = alternateLink
        )
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

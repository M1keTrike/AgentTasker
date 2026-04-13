package com.agentasker.features.classroom.domain.usecases

import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import javax.inject.Inject

class SyncClassroomToTasksUseCase @Inject constructor(
    private val classroomRepository: ClassroomRepository
) {
    suspend operator fun invoke(): Result<Int> =
        classroomRepository.syncClassroomTasksToLocal()

    suspend operator fun invoke(courseIds: List<String>): Result<Int> =
        classroomRepository.syncClassroomTasksByCourses(courseIds)
}

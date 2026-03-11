package com.agentasker.features.classroom.domain.usecases

import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import javax.inject.Inject

class GetClassroomTasksUseCase @Inject constructor(
    private val repository: ClassroomRepository
) {
    suspend operator fun invoke(courseId: String? = null): Result<List<ClassroomTask>> {
        return if (courseId != null) {
            repository.getTasksByCourse(courseId)
        } else {
            repository.getAllTasks()
        }
    }
}

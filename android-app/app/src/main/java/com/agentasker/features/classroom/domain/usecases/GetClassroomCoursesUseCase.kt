package com.agentasker.features.classroom.domain.usecases

import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import javax.inject.Inject

class GetClassroomCoursesUseCase @Inject constructor(
    private val repository: ClassroomRepository
) {
    suspend operator fun invoke(): Result<List<ClassroomCourse>> {
        return repository.getCourses()
    }
}

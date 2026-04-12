package com.agentasker.features.classroom.domain.usecases

import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import javax.inject.Inject

/**
 * Caso de uso del botón "Sincronizar Classroom" del Dashboard.
 *
 * Dos variantes:
 *   - `invoke()` — sync de TODOS los cursos (retrocompat).
 *   - `invoke(courseIds)` — sync selectivo: solo los cursos elegidos
 *      en el picker de cursos del Dashboard.
 */
class SyncClassroomToTasksUseCase @Inject constructor(
    private val classroomRepository: ClassroomRepository
) {
    /** Sincroniza tasks pendientes de TODOS los cursos. */
    suspend operator fun invoke(): Result<Int> =
        classroomRepository.syncClassroomTasksToLocal()

    /** Sincroniza tasks pendientes solo de los cursos seleccionados. */
    suspend operator fun invoke(courseIds: List<String>): Result<Int> =
        classroomRepository.syncClassroomTasksByCourses(courseIds)
}

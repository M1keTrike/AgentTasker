package com.agentasker.features.classroom.domain.usecases

import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import javax.inject.Inject

/**
 * Caso de uso del botón "Sincronizar tareas de Classroom" del Dashboard.
 * Delega en el repositorio que filtra las pendientes y las importa al
 * modelo local.
 */
class SyncClassroomToTasksUseCase @Inject constructor(
    private val classroomRepository: ClassroomRepository
) {
    suspend operator fun invoke(): Result<Int> = classroomRepository.syncClassroomTasksToLocal()
}

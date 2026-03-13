package com.agentasker.features.classroom.domain.usecases

import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import javax.inject.Inject

class ConnectClassroomUseCase @Inject constructor(
    private val repository: ClassroomRepository
) {
    suspend operator fun invoke(idToken: String, authorizationCode: String, codeVerifier: String? = null): Result<Unit> {
        return repository.connectClassroom(idToken, authorizationCode, codeVerifier)
    }
}

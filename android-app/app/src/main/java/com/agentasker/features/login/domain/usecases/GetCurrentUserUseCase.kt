package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User? {
        return authRepository.getCurrentUser()
    }
}


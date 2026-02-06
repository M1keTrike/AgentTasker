package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.repositories.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.clearAuthToken()
    }
}


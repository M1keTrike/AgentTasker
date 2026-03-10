package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User? {
        return authRepository.getCurrentUser()
    }
}


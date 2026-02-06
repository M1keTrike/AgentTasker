package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.repositories.AuthRepository
import com.agentasker.features.login.domain.validators.AuthValidator

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, email: String, password: String): Result<Unit> {
        AuthValidator.validateUsername(username).getOrElse { return Result.failure(it) }
        AuthValidator.validateEmail(email).getOrElse { return Result.failure(it) }
        AuthValidator.validatePassword(password).getOrElse { return Result.failure(it) }

        return authRepository.register(username, email, password)
    }
}


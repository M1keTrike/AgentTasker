package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository
import com.agentasker.features.login.domain.validators.AuthValidator

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        AuthValidator.validateUsername(username).getOrElse { return Result.failure(it) }
        AuthValidator.validatePassword(password).getOrElse { return Result.failure(it) }

        return authRepository.login(username, password)
    }
}

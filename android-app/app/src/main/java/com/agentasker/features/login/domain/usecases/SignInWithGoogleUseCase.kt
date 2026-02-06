package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return try {
            authRepository.signInWithGoogle(idToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


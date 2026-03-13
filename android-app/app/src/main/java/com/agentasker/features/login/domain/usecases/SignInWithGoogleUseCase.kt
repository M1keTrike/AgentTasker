package com.agentasker.features.login.domain.usecases

import android.content.Context
import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository
import com.agentasker.features.login.domain.services.GoogleSignInProvider
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthProvider: GoogleSignInProvider
) {
    suspend operator fun invoke(context: Context): Result<User> {
        return try {
            val idToken = googleAuthProvider.signInWithGoogle(context)
            authRepository.signInWithGoogle(idToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.agentasker.features.login.domain.usecases

import android.content.Context
import com.agentasker.features.login.data.services.GoogleAuthService
import com.agentasker.features.login.domain.repositories.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val googleAuthService: GoogleAuthService
) {
    suspend operator fun invoke(context: Context) {
        // Limpiar tokens del backend
        authRepository.clearAuthToken()
        // Limpiar credenciales de Google (Firebase + Credential Manager)
        googleAuthService.signOut(context)
    }
}


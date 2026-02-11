package com.agentasker.features.login.domain.usecases

import android.content.Context
import com.agentasker.features.login.data.services.GoogleAuthService
import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val googleAuthService: GoogleAuthService
) {
    /**
     * Ejecuta el flujo completo de autenticación con Google:
     * 1. Obtiene credenciales con Credential Manager
     * 2. Autentica en Firebase
     * 3. Sincroniza con el backend
     *
     * @param context Contexto de la aplicación
     * @return Result con el User si es exitoso
     */
    suspend operator fun invoke(context: Context): Result<User> {
        return try {
            android.util.Log.d("SignInWithGoogleUseCase", "Iniciando flujo de Google Sign In")

            // Obtener ID Token usando Credential Manager + Firebase
            val idToken = googleAuthService.signInWithGoogle(context)
            android.util.Log.d("SignInWithGoogleUseCase", "ID Token obtenido, sincronizando con backend...")

            // Sincronizar con el backend
            val result = authRepository.signInWithGoogle(idToken)

            android.util.Log.d("SignInWithGoogleUseCase", "Sincronización con backend completada: ${result.isSuccess}")

            result
        } catch (e: Exception) {
            android.util.Log.e("SignInWithGoogleUseCase", "Error en el flujo de Google Sign In", e)
            Result.failure(e)
        }
    }
}


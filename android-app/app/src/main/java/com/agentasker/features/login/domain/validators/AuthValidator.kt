package com.agentasker.features.login.domain.validators

import android.util.Patterns

object AuthValidator {

    fun validateUsername(username: String): Result<Unit> {
        return when {
            username.isBlank() -> Result.failure(
                Exception("El nombre de usuario es requerido")
            )
            username.length < 3 -> Result.failure(
                Exception("El nombre de usuario debe tener al menos 3 caracteres")
            )
            else -> Result.success(Unit)
        }
    }

    fun validateEmail(email: String): Result<Unit> {
        return when {
            email.isBlank() -> Result.failure(
                Exception("El email es requerido")
            )
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Result.failure(
                Exception("Email inválido")
            )
            else -> Result.success(Unit)
        }
    }

    fun validatePassword(password: String): Result<Unit> {
        return when {
            password.isBlank() -> Result.failure(
                Exception("La contraseña es requerida")
            )
            password.length < 6 -> Result.failure(
                Exception("La contraseña debe tener al menos 6 caracteres")
            )
            else -> Result.success(Unit)
        }
    }
}


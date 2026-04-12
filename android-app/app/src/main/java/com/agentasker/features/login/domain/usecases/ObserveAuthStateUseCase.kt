package com.agentasker.features.login.domain.usecases

import com.agentasker.features.login.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observa reactivamente si hay sesión válida. Emite `false` cuando
 * el [com.agentasker.core.network.TokenAuthenticator] llama `clearAll()`
 * tras un 401 irrecuperable (por ejemplo, backend reiniciado con nuevo
 * JWT_SECRET).
 *
 * El [LoginViewModel] lo colecciona para forzar navegación a Login
 * sin que el usuario quede atrapado en un Dashboard con 401 permanentes.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = authRepository.observeIsLoggedIn()
}

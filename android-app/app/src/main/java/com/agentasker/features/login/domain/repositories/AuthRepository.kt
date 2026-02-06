package com.agentasker.features.login.domain.repositories

import com.agentasker.features.login.domain.entities.AuthToken
import com.agentasker.features.login.domain.entities.User

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<User>

    suspend fun login(username: String, password: String): Result<User>
    suspend fun register(username: String, email: String, password: String): Result<Unit>

    suspend fun getCurrentUser(): User?
    suspend fun saveAuthToken(token: AuthToken)
    suspend fun getAuthToken(): AuthToken?
    suspend fun clearAuthToken()
    suspend fun isUserLoggedIn(): Boolean
}


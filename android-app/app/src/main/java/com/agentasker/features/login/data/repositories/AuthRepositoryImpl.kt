package com.agentasker.features.login.data.repositories

import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.login.data.datasources.local.SecureTokenStorage
import com.agentasker.features.login.data.datasources.remote.mapper.toDomain
import com.agentasker.features.login.data.datasources.remote.mapper.toDomainFromLogin
import com.agentasker.features.login.data.datasources.remote.model.GoogleSignInRequestDTO
import com.agentasker.features.login.data.datasources.remote.model.LoginRequestDTO
import com.agentasker.features.login.data.datasources.remote.model.RegisterRequestDTO
import com.agentasker.features.login.domain.entities.AuthToken
import com.agentasker.features.login.domain.entities.User
import com.agentasker.features.login.domain.repositories.AuthRepository

class AuthRepositoryImpl(
    private val api: AgentTaskerApi,
    private val secureStorage: SecureTokenStorage
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val request = GoogleSignInRequestDTO(idToken = idToken)
            val response = api.signInWithGoogle(request)

            val user = response.user.toDomain()
            val token = response.token.toDomain()

            secureStorage.saveAuthToken(token)
            secureStorage.saveUser(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            val request = LoginRequestDTO(username = username, password = password)
            val response = api.login(request)

            val user = response.toDomainFromLogin()
            val token = AuthToken(
                accessToken = response.accessToken,
                idToken = null,
                refreshToken = null,
                expiresIn = 86400L
            )

            secureStorage.saveAuthToken(token)
            secureStorage.saveUser(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(username: String, email: String, password: String): Result<Unit> {
        return try {
            val request = RegisterRequestDTO(
                username = username,
                email = email,
                password = password
            )
            api.register(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        return secureStorage.getUser()
    }

    override suspend fun saveAuthToken(token: AuthToken) {
        secureStorage.saveAuthToken(token)
    }

    override suspend fun getAuthToken(): AuthToken? {
        return secureStorage.getAuthToken()
    }

    override suspend fun clearAuthToken() {
        secureStorage.clearAll()
    }

    override suspend fun isUserLoggedIn(): Boolean {
        val user = secureStorage.getUser()
        val tokenValid = secureStorage.isTokenValid()
        return user != null && tokenValid
    }
}


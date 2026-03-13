package com.agentasker.core.network

import android.util.Log
import com.agentasker.BuildConfig
import com.agentasker.features.login.data.datasources.local.SecureDataStoreTokenStorage
import com.agentasker.features.login.data.datasources.remote.model.RefreshTokenRequestDTO
import com.agentasker.features.login.data.datasources.remote.model.RefreshTokenResponseDTO
import com.agentasker.features.login.domain.entities.AuthToken
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TokenAuthenticator(
    private val tokenStorage: SecureDataStoreTokenStorage
) : Authenticator {

    private val refreshApi: AgentTaskerApi by lazy {
        val client = OkHttpClient.Builder().build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgentTaskerApi::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite retry loops
        if (response.request.header("X-Retry-Auth") != null) {
            return null
        }

        return runBlocking {
            try {
                val currentToken = tokenStorage.getAuthToken()
                val refreshToken = currentToken?.refreshToken
                val accessToken = currentToken?.accessToken

                if (refreshToken == null || accessToken == null) {
                    Log.w(TAG, "No refresh token available, clearing session")
                    tokenStorage.clearAll()
                    return@runBlocking null
                }

                val refreshResponse = refreshApi.refreshToken(
                    RefreshTokenRequestDTO(
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                )

                val newToken = AuthToken(
                    accessToken = refreshResponse.accessToken,
                    idToken = currentToken.idToken,
                    refreshToken = refreshResponse.refreshToken,
                    expiresIn = refreshResponse.expiresIn
                )
                tokenStorage.saveAuthToken(newToken)

                Log.d(TAG, "Token refreshed successfully")

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                    .header("X-Retry-Auth", "true")
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token, clearing session", e)
                tokenStorage.clearAll()
                null
            }
        }
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
    }
}

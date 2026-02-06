package com.agentasker.core.network

import com.agentasker.features.login.data.datasources.local.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStorage: SecureTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getAuthToken()

        val request = if (token != null && tokenStorage.isTokenValid()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${token.accessToken}")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}


package com.agentasker.core.network

import com.agentasker.features.login.data.datasources.local.SecureDataStoreTokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStorage: SecureDataStoreTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = tokenStorage.getCachedAccessToken()

        val request = if (accessToken != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}

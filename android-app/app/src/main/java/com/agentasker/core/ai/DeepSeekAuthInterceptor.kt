package com.agentasker.core.ai

import com.agentasker.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class DeepSeekAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = BuildConfig.DEEPSEEK_API_KEY
        if (key.isBlank()) {
            throw IllegalStateException(
                "DEEPSEEK_API_KEY vacía. Añade `deepseek.api.key=sk-...` en local.properties y rebuild."
            )
        }
        val authed = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(authed)
    }
}

package com.agentasker.core.di

import android.content.Context
import com.agentasker.BuildConfig
import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.login.data.datasources.local.SecureTokenStorage
import com.agentasker.features.tasks.data.repositories.TaskRepositoryImpl
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    private val _baseUrl = BuildConfig.API_BASE_URL

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(com.agentasker.core.network.AuthInterceptor(secureTokenStorage))
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(_baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val agentTaskerApi: AgentTaskerApi by lazy {
        retrofit.create(AgentTaskerApi::class.java)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(agentTaskerApi)
    }

    val secureTokenStorage: SecureTokenStorage by lazy {
        SecureTokenStorage(context)
    }
}


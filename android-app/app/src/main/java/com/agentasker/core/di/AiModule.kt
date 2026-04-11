package com.agentasker.core.di

import com.agentasker.BuildConfig
import com.agentasker.core.ai.DeepSeekApi
import com.agentasker.core.ai.DeepSeekAuthInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * DI para el cliente DeepSeek. Se mantiene aparte de `NetworkModule` porque
 * la base URL y la autenticación son distintas (Bearer con una API key
 * estática vs. los JWT del backend de AgentTasker).
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideDeepSeekAuthInterceptor(): DeepSeekAuthInterceptor = DeepSeekAuthInterceptor()

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekOkHttpClient(
        authInterceptor: DeepSeekAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        // DeepSeek puede tardar en responder con prompts grandes. 90s
        // para read/write cubre el p99 sin frustrar al usuario.
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(
        @Named("deepseek") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DEEPSEEK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(@Named("deepseek") retrofit: Retrofit): DeepSeekApi {
        return retrofit.create(DeepSeekApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

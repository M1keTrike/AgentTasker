package com.agentasker.core.di

import android.content.Context
import com.agentasker.BuildConfig
import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.core.network.AuthInterceptor
import com.agentasker.core.network.ConnectivityManagerNetworkMonitor
import com.agentasker.core.network.TokenAuthenticator
import com.agentasker.core.network.NetworkMonitor
import com.agentasker.features.login.data.datasources.local.SecureDataStoreTokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(secureDataStoreTokenStorage: SecureDataStoreTokenStorage): AuthInterceptor {
        return AuthInterceptor(secureDataStoreTokenStorage)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(secureDataStoreTokenStorage: SecureDataStoreTokenStorage): TokenAuthenticator {
        return TokenAuthenticator(secureDataStoreTokenStorage)
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // En debug mostramos request + response completos para depurar
            // sync fails; en release bajamos a BASIC para no filtrar PII.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAgentTaskerApi(retrofit: Retrofit): AgentTaskerApi {
        return retrofit.create(AgentTaskerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return ConnectivityManagerNetworkMonitor(context)
    }
}

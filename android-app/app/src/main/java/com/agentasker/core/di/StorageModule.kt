package com.agentasker.core.di

import android.content.Context
import com.agentasker.features.login.data.datasources.local.SecureTokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideSecureTokenStorage(@ApplicationContext context: Context): SecureTokenStorage {
        return SecureTokenStorage(context)
    }
}

package com.agentasker.features.login.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.agentasker.features.login.data.repositories.AuthRepositoryImpl
import com.agentasker.features.login.data.services.GoogleAuthService
import com.agentasker.features.login.domain.repositories.AuthRepository
import com.agentasker.features.login.domain.services.GoogleSignInProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGoogleSignInProvider(impl: GoogleAuthService): GoogleSignInProvider

    companion object {

        @Provides
        @Singleton
        fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
            return CredentialManager.create(context)
        }

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }
    }
}

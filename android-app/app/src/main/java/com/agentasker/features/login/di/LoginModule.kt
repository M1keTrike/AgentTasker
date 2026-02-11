package com.agentasker.features.login.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.agentasker.core.di.AppContainer
import com.agentasker.features.login.data.datasources.local.SecureTokenStorage
import com.agentasker.features.login.data.repositories.AuthRepositoryImpl
import com.agentasker.features.login.data.services.GoogleAuthService
import com.agentasker.features.login.domain.repositories.AuthRepository
import com.agentasker.features.login.domain.usecases.GetCurrentUserUseCase
import com.agentasker.features.login.domain.usecases.LoginUseCase
import com.agentasker.features.login.domain.usecases.RegisterUseCase
import com.agentasker.features.login.domain.usecases.SignInWithGoogleUseCase
import com.agentasker.features.login.domain.usecases.SignOutUseCase
import com.agentasker.features.login.presentation.viewmodel.LoginViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class LoginModule(
    private val appContainer: AppContainer,
    private val secureTokenStorage: SecureTokenStorage,
    private val context: Context
) {

    private fun provideCredentialManager(): CredentialManager {
        return CredentialManager.create(context)
    }

    private fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    private fun provideGoogleAuthService(): GoogleAuthService {
        return GoogleAuthService(
            credentialManager = provideCredentialManager(),
            firebaseAuth = provideFirebaseAuth()
        )
    }

    private fun provideAuthRepository(): AuthRepository {
        return AuthRepositoryImpl(
            api = appContainer.agentTaskerApi,
            secureStorage = secureTokenStorage
        )
    }

    private fun provideSignInWithGoogleUseCase(): SignInWithGoogleUseCase {
        return SignInWithGoogleUseCase(
            authRepository = provideAuthRepository(),
            googleAuthService = provideGoogleAuthService()
        )
    }

    private fun provideLoginUseCase(): LoginUseCase {
        return LoginUseCase(provideAuthRepository())
    }

    private fun provideRegisterUseCase(): RegisterUseCase {
        return RegisterUseCase(provideAuthRepository())
    }

    private fun provideGetCurrentUserUseCase(): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(provideAuthRepository())
    }

    private fun provideSignOutUseCase(): SignOutUseCase {
        return SignOutUseCase(
            authRepository = provideAuthRepository(),
            googleAuthService = provideGoogleAuthService()
        )
    }


    fun provideLoginViewModelFactory(): LoginViewModelFactory {
        return LoginViewModelFactory(
            loginUseCase = provideLoginUseCase(),
            registerUseCase = provideRegisterUseCase(),
            signOutUseCase = provideSignOutUseCase(),
            signInWithGoogleUseCase = provideSignInWithGoogleUseCase(),
            getCurrentUserUseCase = provideGetCurrentUserUseCase(),
            context = context
        )
    }
}


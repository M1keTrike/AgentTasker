package com.agentasker.features.login.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agentasker.features.login.domain.usecases.GetCurrentUserUseCase
import com.agentasker.features.login.domain.usecases.LoginUseCase
import com.agentasker.features.login.domain.usecases.RegisterUseCase
import com.agentasker.features.login.domain.usecases.SignInWithGoogleUseCase
import com.agentasker.features.login.domain.usecases.SignOutUseCase

class LoginViewModelFactory(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginUseCase = loginUseCase,
                registerUseCase = registerUseCase,
                signOutUseCase = signOutUseCase,
                signInWithGoogleUseCase = signInWithGoogleUseCase,
                getCurrentUserUseCase = getCurrentUserUseCase,
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


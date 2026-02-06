package com.agentasker.features.login.presentation.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.login.domain.usecases.LoginUseCase
import com.agentasker.features.login.domain.usecases.RegisterUseCase
import com.agentasker.features.login.domain.usecases.SignOutUseCase
import com.agentasker.features.login.presentation.screens.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    companion object {
        private const val GOOGLE_CLIENT_ID = "demo"
        private const val REDIRECT_URI = "com.agentasker://oauth2redirect"
        private const val OAUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val SCOPES = "openid%20email%20profile"
    }

    fun login(username: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            loginUseCase(username, password)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = user,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al iniciar sesión"
                    )
                }
        }
    }

    fun register(username: String, email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            registerUseCase(username, email, password)
                .onSuccess {
                    login(username, password)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al registrarse"
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _uiState.value = LoginUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isLoginPasswordVisible = !_uiState.value.isLoginPasswordVisible
        )
    }

    fun toggleRegisterPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isRegisterPasswordVisible = !_uiState.value.isRegisterPasswordVisible
        )
    }

    fun toggleRegisterMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            username = "",
            email = "",
            password = ""
        )
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun createGoogleSignInIntent(): Intent {
        val authUrl = buildString {
            append(OAUTH_BASE_URL)
            append("?client_id=$GOOGLE_CLIENT_ID")
            append("&redirect_uri=$REDIRECT_URI")
            append("&response_type=code")
            append("&scope=$SCOPES")
        }

        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(authUrl)
        }
    }
}


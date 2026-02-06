package com.agentasker.features.login.presentation.screens

import com.agentasker.features.login.domain.entities.User

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isLoginPasswordVisible: Boolean = false,
    val isRegisterPasswordVisible: Boolean = false,
    val isRegisterMode: Boolean = false,
    val username: String = "",
    val email: String = "",
    val password: String = ""
)


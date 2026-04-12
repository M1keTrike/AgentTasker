package com.agentasker.features.login.presentation.viewmodel

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.login.domain.usecases.GetCurrentUserUseCase
import com.agentasker.features.login.domain.usecases.LoginUseCase
import com.agentasker.features.login.domain.usecases.ObserveAuthStateUseCase
import com.agentasker.features.login.domain.usecases.RegisterUseCase
import com.agentasker.features.login.domain.usecases.SignInWithGoogleUseCase
import com.agentasker.features.login.domain.usecases.SignOutUseCase
import com.agentasker.features.login.presentation.screens.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkAuthenticationState()
        observeSessionValidity()
    }

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserUseCase()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        currentUser = user
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Observa el DataStore de tokens. Cuando el [TokenAuthenticator]
     * llama `clearAll()` tras un 401 irrecuperable (backend reiniciado,
     * JWT_SECRET cambiado, etc.), el Flow emite `false` y nosotros
     * reseteamos [LoginUiState] a no-autenticado. Esto fuerza la
     * navegación al LoginRoute en [AgentTaskerApp] porque `startDestination`
     * se re-evalúa cuando `isAuthenticated` cambia.
     */
    private fun observeSessionValidity() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { isLoggedIn ->
                if (!isLoggedIn && _uiState.value.isAuthenticated) {
                    _uiState.value = LoginUiState() // reset completo
                }
            }
        }
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

    fun signInWithGoogle(context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                signInWithGoogleUseCase(context)
                    .onSuccess { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = user,
                            error = null
                        )
                    }
                    .onFailure { exception ->
                        val errorMessage = when (exception) {
                            is GetCredentialCancellationException -> "Inicio de sesión cancelado"
                            is NoCredentialException -> "No hay cuentas de Google disponibles"
                            is GetCredentialException -> "Error al obtener credenciales: ${exception.message}"
                            else -> exception.message ?: "Error al iniciar sesión con Google"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error inesperado"
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
            signOutUseCase(context)
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
}

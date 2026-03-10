package com.agentasker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agentasker.core.navigation.NavigatorWrapper
import com.agentasker.core.navigation.Screen
import com.agentasker.core.ui.theme.AgenTaskerTheme
import com.agentasker.features.login.presentation.screens.LoginScreen
import com.agentasker.features.login.presentation.viewmodel.LoginViewModel
import com.agentasker.features.tasks.presentation.screens.TaskScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AgenTaskerTheme {
                AgentTaskerApp()
            }
        }
    }
}

@Composable
fun AgentTaskerApp() {
    val navController = rememberNavController()
    val navigatorWrapper = remember { NavigatorWrapper(navController) }

    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginUiState by loginViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (loginUiState.isAuthenticated) {
        Screen.Tasks.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    android.util.Log.d("MainActivity", "onLoginSuccess callback ejecutado, navegando a Tasks")
                    navigatorWrapper.navigateToTasks(clearBackStack = true)
                    android.util.Log.d("MainActivity", "Navegación a Tasks completada")
                }
            )
        }

        composable(Screen.Tasks.route) {
            TaskScreen()
        }
    }
}

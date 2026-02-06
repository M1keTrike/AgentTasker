package com.agentasker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agentasker.core.navigation.NavigatorWrapper
import com.agentasker.core.navigation.Screen
import com.agentasker.core.ui.theme.AgenTaskerTheme
import com.agentasker.features.login.di.LoginModule
import com.agentasker.features.login.presentation.screens.LoginScreen
import com.agentasker.features.login.presentation.viewmodel.LoginViewModel
import com.agentasker.features.tasks.di.TasksModule
import com.agentasker.features.tasks.presentation.screens.TaskScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as AgentTaskerApplication).appContainer

        setContent {
            AgenTaskerTheme {
                AgentTaskerApp(appContainer)
            }
        }
    }
}

@Composable
fun AgentTaskerApp(appContainer: com.agentasker.core.di.AppContainer) {
    val navController = rememberNavController()
    val navigatorWrapper = remember { NavigatorWrapper(navController) }

    val loginModule = remember { LoginModule(appContainer, appContainer.secureTokenStorage) }
    val loginViewModel: LoginViewModel = viewModel(
        factory = loginModule.provideLoginViewModelFactory()
    )
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
                    navigatorWrapper.navigateToTasks(clearBackStack = true)
                }
            )
        }

        composable(Screen.Tasks.route) {
            TaskScreen(factory = TasksModule(appContainer).provideTaskViewModelFactory())
        }
    }
}




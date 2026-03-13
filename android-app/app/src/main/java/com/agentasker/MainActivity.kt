package com.agentasker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agentasker.core.navigation.NavigatorWrapper
import com.agentasker.core.navigation.Screen
import com.agentasker.core.network.NetworkMonitor
import com.agentasker.core.ui.components.OfflineBanner
import com.agentasker.core.ui.theme.AgenTaskerTheme
import com.agentasker.features.classroom.data.services.ClassroomAuthService
import com.agentasker.features.classroom.presentation.screens.ClassroomScreen
import com.agentasker.features.dashboard.presentation.screens.DashboardScreen
import com.agentasker.features.login.presentation.screens.LoginScreen
import com.agentasker.features.login.presentation.viewmodel.LoginViewModel
import com.agentasker.features.tasks.presentation.screens.TaskScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var classroomAuthService: ClassroomAuthService

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AgenTaskerTheme {
                AgentTaskerApp(
                    classroomAuthService = classroomAuthService,
                    networkMonitor = networkMonitor
                )
            }
        }
    }
}

@Composable
fun AgentTaskerApp(
    classroomAuthService: ClassroomAuthService,
    networkMonitor: NetworkMonitor
) {
    val navController = rememberNavController()
    val navigatorWrapper = remember { NavigatorWrapper(navController) }

    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginUiState by loginViewModel.uiState.collectAsStateWithLifecycle()

    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    val startDestination = if (loginUiState.isAuthenticated) {
        Screen.Dashboard.route
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
                    navigatorWrapper.navigateToDashboard(clearBackStack = true)
                }
            )
        }

        composable(Screen.Dashboard.route) {
            MainScaffold(navController = navController, currentRoute = Screen.Dashboard.route, isOffline = !isOnline) {
                DashboardScreen(
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToClassroom = {
                        navController.navigate(Screen.Classroom.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        composable(Screen.Tasks.route) {
            MainScaffold(navController = navController, currentRoute = Screen.Tasks.route, isOffline = !isOnline) {
                TaskScreen()
            }
        }

        composable(Screen.Classroom.route) {
            MainScaffold(navController = navController, currentRoute = Screen.Classroom.route, isOffline = !isOnline) {
                ClassroomScreen(classroomAuthService = classroomAuthService)
            }
        }
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    currentRoute: String,
    isOffline: Boolean = false,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Screen.Dashboard.route,
                    onClick = {
                        if (currentRoute != Screen.Dashboard.route) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.Tasks.route,
                    onClick = {
                        if (currentRoute != Screen.Tasks.route) {
                            navController.navigate(Screen.Tasks.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Outlined.TaskAlt, contentDescription = "Tareas") },
                    label = { Text("Tareas") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.Classroom.route,
                    onClick = {
                        if (currentRoute != Screen.Classroom.route) {
                            navController.navigate(Screen.Classroom.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Outlined.School, contentDescription = "Classroom") },
                    label = { Text("Classroom") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OfflineBanner(isOffline = isOffline)
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

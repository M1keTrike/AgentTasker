package com.agentasker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agentasker.core.navigation.ClassroomRoute
import com.agentasker.core.navigation.DashboardRoute
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.core.navigation.KanbanRoute
import com.agentasker.core.navigation.LoginRoute
import com.agentasker.core.navigation.TasksRoute
import com.agentasker.core.network.NetworkMonitor
import com.agentasker.core.notifications.DeepLink
import com.agentasker.core.ui.components.OfflineBanner
import com.agentasker.core.ui.theme.AgenTaskerTheme
import com.agentasker.features.login.presentation.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var featureNavGraphs: Set<@JvmSuppressWildcards FeatureNavGraph>

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted: no-op; si se niega, el usuario simplemente no verá pushes */ }

    /**
     * Deep link pendiente de procesar. Se llena cuando la Activity arranca
     * desde una notificación o cuando recibe `onNewIntent` con un payload
     * que contiene `screen=...`. El Composable lo observa y navega.
     */
    private val pendingDeepLink: MutableStateFlow<DeepLink?> = MutableStateFlow(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ensureNotificationPermission()
        pendingDeepLink.value = DeepLink.fromIntent(intent)

        setContent {
            AgenTaskerTheme {
                AgentTaskerApp(
                    featureNavGraphs = featureNavGraphs,
                    networkMonitor = networkMonitor,
                    pendingDeepLink = pendingDeepLink.asStateFlow(),
                    onDeepLinkConsumed = { pendingDeepLink.value = null }
                )
            }
        }
    }

    /**
     * Se invoca cuando la Activity ya estaba creada (launchMode=singleTop)
     * y Android reentrega un intent — por ejemplo cuando el usuario toca
     * una notificación estando la app en foreground o background.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLink.fromIntent(intent)?.let { pendingDeepLink.value = it }
    }

    /**
     * Pide el permiso `POST_NOTIFICATIONS` en tiempo de ejecución (Android 13+).
     * En versiones anteriores el permiso se concede automáticamente en el Manifest.
     */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun AgentTaskerApp(
    featureNavGraphs: Set<FeatureNavGraph>,
    networkMonitor: NetworkMonitor,
    pendingDeepLink: StateFlow<DeepLink?>,
    onDeepLinkConsumed: () -> Unit
) {
    val navController = rememberNavController()

    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginUiState by loginViewModel.uiState.collectAsStateWithLifecycle()

    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    val deepLink by pendingDeepLink.collectAsState()

    // Navegación reactiva a un deep link. Solo dispara cuando el usuario ya
    // está autenticado (si no, lo dejamos pendiente hasta que haga login).
    LaunchedEffect(deepLink, loginUiState.isAuthenticated) {
        val target = deepLink ?: return@LaunchedEffect
        if (!loginUiState.isAuthenticated) return@LaunchedEffect

        val route: Any = when (target) {
            DeepLink.Dashboard -> DashboardRoute
            DeepLink.Tasks -> TasksRoute
            DeepLink.Kanban -> KanbanRoute
            DeepLink.Classroom -> ClassroomRoute
        }

        navController.navigate(route) {
            popUpTo(DashboardRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onDeepLinkConsumed()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isLoginScreen = currentDestination?.hasRoute<LoginRoute>() == true

    val startDestination: Any = if (loginUiState.isAuthenticated) {
        DashboardRoute
    } else {
        LoginRoute
    }

    Scaffold(
        bottomBar = {
            if (!isLoginScreen) {
                BottomNavBar(navController = navController, currentDestination = currentDestination)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!isLoginScreen) {
                OfflineBanner(isOffline = !isOnline)
            }
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    featureNavGraphs.forEach { featureNavGraph ->
                        featureNavGraph.registerGraph(
                            navGraphBuilder = this,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val textAlign: TextAlign? = null
)

@Composable
private fun BottomNavBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val items = listOf(
        BottomNavItem("Panel de estado", Icons.Outlined.Dashboard, DashboardRoute, TextAlign.Center),
        BottomNavItem("Tareas", Icons.Outlined.TaskAlt, TasksRoute),
        BottomNavItem("Kanban", Icons.Outlined.ViewColumn, KanbanRoute),
        BottomNavItem("Classroom", Icons.Outlined.School, ClassroomRoute)
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    Text(
                        text = item.label,
                        textAlign = item.textAlign ?: TextAlign.Unspecified
                    )
                }
            )
        }
    }
}

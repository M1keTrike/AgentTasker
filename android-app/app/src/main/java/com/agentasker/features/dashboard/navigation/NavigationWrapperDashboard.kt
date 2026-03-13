package com.agentasker.features.dashboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentasker.core.navigation.ClassroomRoute
import com.agentasker.core.navigation.DashboardRoute
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.core.navigation.TasksRoute
import com.agentasker.features.dashboard.presentation.screens.DashboardScreen
import javax.inject.Inject

class NavigationWrapperDashboard @Inject constructor() : FeatureNavGraph {

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController
    ) {
        navGraphBuilder.composable<DashboardRoute> {
            DashboardScreen(
                onNavigateToTasks = {
                    navController.navigate(TasksRoute) {
                        popUpTo(DashboardRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToClassroom = {
                    navController.navigate(ClassroomRoute) {
                        popUpTo(DashboardRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

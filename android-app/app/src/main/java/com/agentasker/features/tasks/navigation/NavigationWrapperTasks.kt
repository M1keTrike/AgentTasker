package com.agentasker.features.tasks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.core.navigation.TasksRoute
import com.agentasker.features.tasks.presentation.screens.TaskScreen
import javax.inject.Inject

class NavigationWrapperTasks @Inject constructor() : FeatureNavGraph {

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController
    ) {
        navGraphBuilder.composable<TasksRoute> {
            TaskScreen()
        }
    }
}

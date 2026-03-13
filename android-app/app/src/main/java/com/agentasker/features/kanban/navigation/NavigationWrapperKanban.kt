package com.agentasker.features.kanban.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.core.navigation.KanbanRoute
import com.agentasker.features.kanban.presentation.screens.KanbanScreen
import javax.inject.Inject

class NavigationWrapperKanban @Inject constructor() : FeatureNavGraph {

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController
    ) {
        navGraphBuilder.composable<KanbanRoute> {
            KanbanScreen()
        }
    }
}

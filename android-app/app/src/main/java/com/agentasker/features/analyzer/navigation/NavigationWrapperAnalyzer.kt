package com.agentasker.features.analyzer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentasker.core.navigation.AnalyzerRoute
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.features.analyzer.presentation.screens.AnalyzerScreen
import javax.inject.Inject

class NavigationWrapperAnalyzer @Inject constructor() : FeatureNavGraph {

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController
    ) {
        navGraphBuilder.composable<AnalyzerRoute> {
            AnalyzerScreen()
        }
    }
}

package com.agentasker.features.classroom.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentasker.core.navigation.ClassroomRoute
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.features.classroom.data.services.ClassroomAuthService
import com.agentasker.features.classroom.presentation.screens.ClassroomScreen
import javax.inject.Inject

class NavigationWrapperClassroom @Inject constructor(
    private val classroomAuthService: ClassroomAuthService
) : FeatureNavGraph {

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController
    ) {
        navGraphBuilder.composable<ClassroomRoute> {
            ClassroomScreen(classroomAuthService = classroomAuthService)
        }
    }
}

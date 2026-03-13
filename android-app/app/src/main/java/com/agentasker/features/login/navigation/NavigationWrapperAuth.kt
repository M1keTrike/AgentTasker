package com.agentasker.features.login.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentasker.core.navigation.DashboardRoute
import com.agentasker.core.navigation.FeatureNavGraph
import com.agentasker.core.navigation.LoginRoute
import com.agentasker.features.login.presentation.screens.LoginScreen
import com.agentasker.features.login.presentation.viewmodel.LoginViewModel
import javax.inject.Inject

class NavigationWrapperAuth @Inject constructor() : FeatureNavGraph {

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController
    ) {
        navGraphBuilder.composable<LoginRoute> {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(DashboardRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

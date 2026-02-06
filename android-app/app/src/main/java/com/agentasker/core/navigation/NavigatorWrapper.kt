package com.agentasker.core.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

class NavigatorWrapper(private val navController: NavHostController) {

    fun navigateTo(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        navController.navigate(route, builder)
    }

    fun navigateToLogin(clearBackStack: Boolean = false) {
        if (clearBackStack) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route)
        }
    }

    fun navigateToTasks(clearBackStack: Boolean = false) {
        if (clearBackStack) {
            navController.navigate(Screen.Tasks.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Tasks.route)
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }
}


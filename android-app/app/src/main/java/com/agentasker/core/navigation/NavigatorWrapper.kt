package com.agentasker.core.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

class NavigatorWrapper(private val navController: NavHostController) {

    fun navigateToTasks(clearBackStack: Boolean = false) {
        if (clearBackStack) {
            navController.navigate(Screen.Tasks.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Tasks.route)
        }
    }

}


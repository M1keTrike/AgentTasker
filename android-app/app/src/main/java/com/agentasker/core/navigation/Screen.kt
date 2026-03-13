package com.agentasker.core.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Tasks : Screen("tasks")
    object Classroom : Screen("classroom")
    object Kanban : Screen("kanban")
}


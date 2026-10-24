package com.voaaguia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.voaaguia.model.repository.ApiSession
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voaaguia.controller.*
import com.voaaguia.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Guidelines : Screen("guidelines")
    object Ideas : Screen("ideas")
    object Projects : Screen("projects")
    object Dashboard : Screen("dashboard")
    object Ranking : Screen("ranking")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Controllers (shared across screens - MVC pattern)
    val authController = remember { AuthController() }
    val currentUser by authController.currentUser.collectAsState()
    val guidelineController = remember(currentUser?.id) { GuidelineController() }
    val ideaController = remember(currentUser?.id) { IdeaController() }
    val projectController = remember(currentUser?.id) { ProjectController() }
    val dashboardController = remember(currentUser?.id) { DashboardController() }
    val geminiController = remember(currentUser?.id) { GeminiController() }

    LaunchedEffect(Unit) {
        ApiSession.expired.collect { expired ->
            if (expired) {
                authController.logout()
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                ApiSession.expired.value = false
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authController = authController,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                authController = authController,
                dashboardController = dashboardController,
                onNavigateToGuidelines = { navController.navigate(Screen.Guidelines.route) },
                onNavigateToIdeas = { navController.navigate(Screen.Ideas.route) },
                onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToRanking = { navController.navigate(Screen.Ranking.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Guidelines.route) {
            GuidelinesScreen(
                authController = authController,
                guidelineController = guidelineController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Ideas.route) {
            IdeasScreen(
                authController = authController,
                ideaController = ideaController,
                geminiController = geminiController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                authController = authController,
                projectController = projectController,
                ideaController = ideaController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                dashboardController = dashboardController,
                geminiController = geminiController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Ranking.route) {
            RankingScreen(
                dashboardController = dashboardController,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

package com.example.citizenreportai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.citizenreportai.data.repository.MockReportRepository
import com.example.citizenreportai.ui.screens.home.HomeScreen
import com.example.citizenreportai.ui.screens.report.CreateReportScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateReport : Screen("create_report")
}

@Composable
fun AppNavigation(repository: MockReportRepository = MockReportRepository()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onNavigateToCreateReport = {
                    navController.navigate(Screen.CreateReport.route)
                }
            )
        }
        composable(Screen.CreateReport.route) {
            CreateReportScreen(
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}


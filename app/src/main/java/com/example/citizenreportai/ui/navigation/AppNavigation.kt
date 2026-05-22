package com.example.citizenreportai.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.citizenreportai.data.repository.RealAuthRepository
import com.example.citizenreportai.data.repository.RealReportRepository
import com.example.citizenreportai.ui.screens.home.HomeScreen
import com.example.citizenreportai.ui.screens.login.LoginScreen
import com.example.citizenreportai.ui.screens.profile.ProfileScreen
import com.example.citizenreportai.ui.screens.report.CreateReportScreen
import com.example.citizenreportai.ui.screens.reports.MyReportsScreen
import com.example.citizenreportai.ui.screens.reports.ReportDetailScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateReport : Screen("create_report")
    object MyReports : Screen("my_reports")
    object ReportDetail : Screen("report_detail/{reportId}") {
        fun createRoute(reportId: String) = "report_detail/$reportId"
    }
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository = remember { RealAuthRepository() }
    val reportRepository = remember { RealReportRepository() }
    
    val currentUser by authRepository.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentUser == null) Screen.Login.route else Screen.Home.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                repository = reportRepository,
                onNavigateToCreateReport = {
                    navController.navigate(Screen.CreateReport.route)
                },
                onNavigateToMyReports = {
                    navController.navigate(Screen.MyReports.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                userRole = currentUser?.role
            )
        }
        composable(Screen.CreateReport.route) {
            CreateReportScreen(
                repository = reportRepository,
                userId = currentUser?.id ?: "",
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.MyReports.route) {
            MyReportsScreen(
                repository = reportRepository,
                userId = currentUser?.id ?: "",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onReportClick = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                }
            )
        }
        composable(Screen.ReportDetail.route) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            ReportDetailScreen(
                repository = reportRepository,
                reportId = reportId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                authRepository = authRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

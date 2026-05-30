package com.example.citizenreportai.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.citizenreportai.data.model.UserRole
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
    val context = LocalContext.current
    val authRepository = remember { RealAuthRepository(context) }
    val reportRepository = remember { RealReportRepository() }
    
    val currentUser by authRepository.currentUser.collectAsState()
    // Funcionario (rol 3) y admin pueden ver y gestionar todos los reportes
    val canManageReports = currentUser?.role == UserRole.FUNCIONARIO || currentUser?.role == UserRole.ADMIN

    // Mapa userId -> nombre del ciudadano, para que el funcionario sepa quién reportó
    var reporterNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(canManageReports) {
        if (canManageReports) {
            reporterNames = authRepository.getUsers().associate { user ->
                val fullName = listOfNotNull(
                    user.firstName.trim().takeIf { it.isNotEmpty() },
                    user.lastName?.trim()?.takeIf { it.isNotEmpty() }
                ).joinToString(" ")
                user.id.toString() to fullName.ifBlank { "Usuario #${user.id}" }
            }
        }
    }

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
                onNavigateToReportDetail = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                },
                userRole = currentUser?.role,
                userFirstName = currentUser?.firstName,
                userId = currentUser?.id?.toString() ?: "",
                canCreateReports = !canManageReports,
                reporterNames = reporterNames
            )
        }
        composable(Screen.CreateReport.route) {
            CreateReportScreen(
                repository = reportRepository,
                userId = currentUser?.id?.toString() ?: "",
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.MyReports.route) {
            MyReportsScreen(
                repository = reportRepository,
                userId = currentUser?.id?.toString() ?: "",
                canManageAll = canManageReports,
                reporterNames = reporterNames,
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
                canManageStatus = canManageReports,
                reporterNames = reporterNames,
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

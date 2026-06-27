package com.taskflow.audit.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taskflow.audit.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StaffHome : Screen("staff_home/{staffId}") {
        fun createRoute(staffId: String) = "staff_home/$staffId"
    }
    object MyTimesheet : Screen("my_timesheet/{staffId}") {
        fun createRoute(staffId: String) = "my_timesheet/$staffId"
    }
    object AdminDashboard : Screen("admin_dashboard/{adminId}") {
        fun createRoute(adminId: String) = "admin_dashboard/$adminId"
    }
    object AdminStaffDetail : Screen("admin_staff/{staffId}") {
        fun createRoute(staffId: String) = "admin_staff/$staffId"
    }
    object AdminEngagements : Screen("admin_engagements")
    object Settings : Screen("settings/{isAdmin}/{userId}") {
        fun createRoute(isAdmin: Boolean, userId: String) = "settings/$isAdmin/$userId"
    }
}

@Composable
fun TaskFlowNavHost(
    navController: NavHostController,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = { staffId, isAdmin ->
                    if (isAdmin) {
                        navController.navigate(Screen.AdminDashboard.createRoute(staffId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StaffHome.createRoute(staffId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.StaffHome.route,
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: return@composable
            StaffHomeScreen(
                staffId = staffId,
                onNavigateToTimesheet = { navController.navigate(Screen.MyTimesheet.createRoute(staffId)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute(false, staffId)) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.MyTimesheet.route,
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: return@composable
            MyTimesheetScreen(
                staffId = staffId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AdminDashboard.route,
            arguments = listOf(navArgument("adminId") { type = NavType.StringType })
        ) { backStackEntry ->
            val adminId = backStackEntry.arguments?.getString("adminId") ?: return@composable
            AdminDashboardScreen(
                adminId = adminId,
                onNavigateToStaffDetail = { staffId -> navController.navigate(Screen.AdminStaffDetail.createRoute(staffId)) },
                onNavigateToEngagements = { navController.navigate(Screen.AdminEngagements.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute(true, adminId)) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.AdminStaffDetail.route,
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: return@composable
            AdminStaffDetailScreen(
                staffId = staffId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminEngagements.route) {
            AdminEngagementsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("isAdmin") { type = NavType.BoolType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                currentTheme = themeMode,
                onThemeChange = onThemeChange
            )
        }
    }
}

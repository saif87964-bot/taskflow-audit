package com.taskflow.audit.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.ui.screens.*
import com.taskflow.audit.ui.viewmodel.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StaffHome : Screen("staff_home/{uid}") {
        fun createRoute(uid: String) = "staff_home/$uid"
    }
    object MyTimesheet : Screen("my_timesheet/{uid}") {
        fun createRoute(uid: String) = "my_timesheet/$uid"
    }
    object Tasks : Screen("tasks/{uid}/{isAdmin}") {
        fun createRoute(uid: String, isAdmin: Boolean) = "tasks/$uid/$isAdmin"
    }
    object Logbook : Screen("logbook/{uid}/{isAdmin}") {
        fun createRoute(uid: String, isAdmin: Boolean) = "logbook/$uid/$isAdmin"
    }
    object AdminDashboard : Screen("admin_dashboard/{uid}") {
        fun createRoute(uid: String) = "admin_dashboard/$uid"
    }
    object AdminStaffDetail : Screen("admin_staff/{staffUid}") {
        fun createRoute(staffUid: String) = "admin_staff/$staffUid"
    }
    object AdminEngagements : Screen("admin_engagements")
    object Settings : Screen("settings/{isAdmin}/{uid}") {
        fun createRoute(isAdmin: Boolean, uid: String) = "settings/$isAdmin/$uid"
    }
    object SetNewPin : Screen("set_new_pin/{uid}/{isAdmin}") {
        fun createRoute(uid: String, isAdmin: Boolean) = "set_new_pin/$uid/$isAdmin"
    }
}

@Composable
fun TaskFlowNavHost(
    navController: NavHostController,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val scope = rememberCoroutineScope()

    fun doLogout() {
        scope.launch {
            AppRepositories.auth.signOut()
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = { uid, isAdmin ->
                    if (isAdmin) {
                        navController.navigate(Screen.AdminDashboard.createRoute(uid)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StaffHome.createRoute(uid)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onPinChange = { uid, isAdmin ->
                    navController.navigate(Screen.SetNewPin.createRoute(uid, isAdmin)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.StaffHome.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { back ->
            val uid = back.arguments?.getString("uid") ?: return@composable
            val vm: StaffHomeViewModel = viewModel(factory = StaffHomeViewModel.Factory(uid))
            StaffHomeScreen(
                vm = vm,
                onNavigateToTimesheet = { navController.navigate(Screen.MyTimesheet.createRoute(uid)) },
                onNavigateToTasks     = { navController.navigate(Screen.Tasks.createRoute(uid, false)) },
                onNavigateToLogbook   = { navController.navigate(Screen.Logbook.createRoute(uid, false)) },
                onNavigateToSettings  = { navController.navigate(Screen.Settings.createRoute(false, uid)) },
                onLogout = { doLogout() }
            )
        }

        composable(
            route = Screen.MyTimesheet.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { back ->
            val uid = back.arguments?.getString("uid") ?: return@composable
            val vm: TimesheetViewModel = viewModel(factory = TimesheetViewModel.Factory(uid))
            MyTimesheetScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Tasks.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("isAdmin") { type = NavType.BoolType }
            )
        ) { back ->
            val uid     = back.arguments?.getString("uid") ?: return@composable
            val isAdmin = back.arguments?.getBoolean("isAdmin") ?: false
            val vm: TasksViewModel = viewModel(factory = TasksViewModel.Factory(uid, isAdmin))
            TasksScreen(vm = vm, isAdmin = isAdmin, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Logbook.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("isAdmin") { type = NavType.BoolType }
            )
        ) { back ->
            val uid     = back.arguments?.getString("uid") ?: return@composable
            val isAdmin = back.arguments?.getBoolean("isAdmin") ?: false
            val vm: LogbookViewModel = viewModel(factory = LogbookViewModel.Factory(uid, isAdmin))
            LogbookScreen(vm = vm, uid = uid, isAdmin = isAdmin, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.AdminDashboard.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { back ->
            val uid = back.arguments?.getString("uid") ?: return@composable
            val vm: AdminViewModel = viewModel(factory = AdminViewModel.Factory(uid))
            AdminDashboardScreen(
                vm = vm,
                onNavigateToStaffDetail = { staffUid -> navController.navigate(Screen.AdminStaffDetail.createRoute(staffUid)) },
                onNavigateToEngagements = { navController.navigate(Screen.AdminEngagements.route) },
                onNavigateToTasks       = { navController.navigate(Screen.Tasks.createRoute(uid, true)) },
                onNavigateToLogbook     = { navController.navigate(Screen.Logbook.createRoute(uid, true)) },
                onNavigateToSettings    = { navController.navigate(Screen.Settings.createRoute(true, uid)) },
                onLogout = { doLogout() }
            )
        }

        composable(
            route = Screen.AdminStaffDetail.route,
            arguments = listOf(navArgument("staffUid") { type = NavType.StringType })
        ) { back ->
            val staffUid = back.arguments?.getString("staffUid") ?: return@composable
            val vm: AdminStaffDetailViewModel = viewModel(factory = AdminStaffDetailViewModel.Factory(staffUid))
            AdminStaffDetailScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.AdminEngagements.route) {
            val vm: AdminEngagementViewModel = viewModel(factory = AdminEngagementViewModel.Factory())
            AdminEngagementsScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("isAdmin") { type = NavType.BoolType },
                navArgument("uid")     { type = NavType.StringType }
            )
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                currentTheme = themeMode,
                onThemeChange = onThemeChange
            )
        }

        composable(
            route = Screen.SetNewPin.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("isAdmin") { type = NavType.BoolType }
            )
        ) { back ->
            val uid = back.arguments?.getString("uid") ?: return@composable
            val isAdmin = back.arguments?.getBoolean("isAdmin") ?: false
            SetNewPinScreen(
                uid = uid,
                isAdmin = isAdmin,
                onComplete = {
                    val dest = if (isAdmin) Screen.AdminDashboard.createRoute(uid)
                               else Screen.StaffHome.createRoute(uid)
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}

package com.mytaskpro.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mytaskpro.ui.MainScreen
import com.mytaskpro.ui.TaskDetailScreen
import com.mytaskpro.SettingsScreen
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel,
    isUserSignedIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                navController = navController,
                taskViewModel = taskViewModel,
                themeViewModel = themeViewModel,
                isUserSignedIn = isUserSignedIn,
                onSettingsClick = { navController.navigate("settings") },
                onTaskClick = { taskId ->
                    navController.navigate("taskDetail/$taskId")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                taskViewModel = taskViewModel,
                themeViewModel = themeViewModel,
                onBackClick = { navController.popBackStack() },
                isUserSignedIn = isUserSignedIn,
                onGoogleSignIn = onGoogleSignIn,
                onSignOut = onSignOut
            )
        }
        composable(
            route = "taskDetail/{taskId}?edit={edit}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.IntType },
                navArgument("edit") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
            val isEditing = backStackEntry.arguments?.getBoolean("edit") ?: false
            TaskDetailScreen(
                taskId = taskId,
                viewModel = taskViewModel,
                onNavigateBack = { navController.popBackStack() },
                isEditing = isEditing
            )
        }
    }
}
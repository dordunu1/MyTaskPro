package com.mytaskpro.ui

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mytaskpro.ui.MainScreen
import com.mytaskpro.ui.TaskDetailScreen
import com.mytaskpro.SettingsScreen
import com.mytaskpro.SettingsViewModel
import com.mytaskpro.billing.BillingManager
import com.mytaskpro.ui.viewmodel.AIRecommendationViewModel
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel,
    isUserSignedIn: Boolean,
    onSettingsClick: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    aiRecommendationViewModel: AIRecommendationViewModel,
    billingManager: BillingManager?,
    activity: Activity
) {
    NavHost(navController = navController, startDestination = "main") {
        composable(route = "main") {
            if (billingManager != null) {
                MainScreen(
                    navController = navController,
                    taskViewModel = taskViewModel,
                    themeViewModel = themeViewModel,
                    isUserSignedIn = isUserSignedIn,
                    onSettingsClick = { navController.navigate("settings") },
                    onTaskClick = { taskId ->
                        navController.navigate("taskDetail/$taskId")
                    },
                    aiRecommendationViewModel = aiRecommendationViewModel,
                    billingManager = billingManager,
                    settingsViewModel = settingsViewModel, // Add this line
                    activity = activity // Add this line
                )
            }
        }
        composable(route = "settings") {
            SettingsScreen(
                taskViewModel = taskViewModel,
                themeViewModel = themeViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                isUserSignedIn = isUserSignedIn,
                onGoogleSignIn = onGoogleSignIn,
                onSignOut = onSignOut,
                activity = activity // Pass the activity here
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
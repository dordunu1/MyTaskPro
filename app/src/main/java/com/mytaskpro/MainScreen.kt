@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytaskpro.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mytaskpro.SettingsViewModel
import com.mytaskpro.billing.BillingManager
import com.mytaskpro.data.Badge
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import com.mytaskpro.data.CategoryType
import com.mytaskpro.ui.components.HorizontalProgressBar
import com.mytaskpro.ui.TaskSummaryGraph
import com.mytaskpro.ui.viewmodel.AIRecommendationViewModel
import kotlinx.coroutines.launch

@Composable
fun PremiumFeatureDialog(
    feature: String,
    description: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade to Premium") },
        text = {
            Column {
                Text("$feature is a premium feature.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(description)
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpgrade()
                onDismiss()
            }) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
fun BadgeInfoDialog(
    currentBadge: Badge,
    tasksCompleted: Int,
    nextBadgeInfo: Pair<Badge, Int>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Current Badge") },
        text = {
            Column {
                Text("Your current badge is: ${currentBadge.name}")
                Text("Tasks completed: $tasksCompleted")
                Text("Next badge: ${nextBadgeInfo.first.name}")
                Text("Tasks needed for next badge: ${nextBadgeInfo.second}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

fun getNextBadge(currentBadge: Badge): String {
    return when (currentBadge) {
        Badge.NONE -> "BRONZE"
        Badge.BRONZE -> "SILVER"
        Badge.SILVER -> "GOLD"
        Badge.GOLD -> "DIAMOND"
        Badge.DIAMOND -> "DIAMOND (Highest)"
    }
}

fun getTasksNeededForNextBadge(currentBadge: Badge, tasksCompleted: Int): Int {
    return when (currentBadge) {
        Badge.NONE -> 30 - tasksCompleted
        Badge.BRONZE -> 80 - tasksCompleted
        Badge.SILVER -> 200 - tasksCompleted
        Badge.GOLD -> 350 - tasksCompleted
        Badge.DIAMOND -> 0
    }.coerceAtLeast(0)
}

@Composable
fun UpgradeToPremiuDialog(
    billingManager: BillingManager,
    activity: Activity,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade to Premium") },
        text = { Text("Upgrade now to access all premium features!") },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch {
                    val productDetails = billingManager.productDetailsFlow.value.firstOrNull()
                    if (productDetails != null) {
                        billingManager.launchBillingFlow(activity, productDetails)
                    }
                }
                onDismiss()
            }) {
                Text("Upgrade")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@ExperimentalMaterial3Api
@Composable
fun MainScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel,
    isUserSignedIn: Boolean,
    onSettingsClick: () -> Unit,
    onTaskClick: (Int) -> Unit,
    aiRecommendationViewModel: AIRecommendationViewModel,
    billingManager: BillingManager,
    settingsViewModel: SettingsViewModel,
    activity: Activity
) {
    val innerNavController = rememberNavController()
    val completionPercentage by taskViewModel.completionPercentage.collectAsState()
    var showGraph by remember { mutableStateOf(false) }
    val currentBadge by taskViewModel.currentBadge.collectAsState()
    val showBadgeAchievement by taskViewModel.showBadgeAchievement.collectAsState()
    var showBadgeInfo by remember { mutableStateOf(false) }
    val completedTaskCount by taskViewModel.completedTaskCount.collectAsState()
    val showConfetti by taskViewModel.showConfetti.collectAsState()
    val tasksCompleted by taskViewModel.tasksCompleted.collectAsState()
    var showUpgradeMessage by remember { mutableStateOf(false) }
    var showTaskSummaryGraphDialog by remember { mutableStateOf(false) }
    var showAchievementBadgesDialog by remember { mutableStateOf(false) }
    val isPremium by settingsViewModel.isPremium.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MyTaskPro")
                        Spacer(modifier = Modifier.width(8.dp))
                        BadgeIcon(
                            badge = currentBadge,
                            onClick = {
                                if (isPremium) {
                                    showBadgeInfo = true
                                } else {
                                    showAchievementBadgesDialog = true
                                }
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // Removed the graph icon from here
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                listOf(
                    Screen.Tasks,
                    Screen.Notes
                ).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            innerNavController.navigate(screen.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalProgressBar(
                    percentage = completionPercentage,
                    height = 8f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (isPremium) {
                            showGraph = true
                        } else {
                            showTaskSummaryGraphDialog = true
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Task Summary",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            NavHost(
                navController = innerNavController,
                startDestination = Screen.Tasks.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Tasks.route) {
                    TasksScreen(
                        viewModel = taskViewModel,
                        settingsViewModel = settingsViewModel,
                        activity = activity,
                        onTaskClick = onTaskClick,
                        onEditTask = { taskId ->
                            innerNavController.navigate("${Screen.EditTask.route}/$taskId")
                        }
                    )
                }

                composable(Screen.Notes.route) {
                    NotesScreen(viewModel = taskViewModel)
                }

                composable(
                    route = "${Screen.EditTask.route}/{taskId}",
                    arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                    EditTaskScreen(
                        taskId = taskId,
                        viewModel = taskViewModel,
                        onNavigateBack = { innerNavController.popBackStack() }
                    )
                }

                composable(
                    route = "${Screen.EditNote.route}/{noteId}",
                    arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
                    EditNoteScreen(
                        viewModel = taskViewModel,
                        noteId = noteId,
                        onNavigateBack = { innerNavController.popBackStack() }
                    )
                }

                composable(
                    route = "note_detail/{noteId}",
                    arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
                    NoteDetailScreen(
                        viewModel = taskViewModel,
                        noteId = noteId,
                        navController = innerNavController
                    )
                }
            }
        }
        if (showConfetti) {
            ConfettiAnimation(
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showUpgradeMessage) {
            UpgradeToPremiuDialog(
                billingManager = billingManager,
                activity = activity,
                onDismiss = { showUpgradeMessage = false }
            )
        }
        showBadgeAchievement?.let { badge ->
            BadgeAchievementPopup(
                badge = badge,
                onDismiss = { taskViewModel.dismissBadgeAchievement() }
            )
        }
        if (showTaskSummaryGraphDialog) {
            PremiumFeatureDialog(
                feature = "Task Summary Graph",
                description = "Visualize your task completion trends and productivity patterns with interactive graphs.",
                onUpgrade = { settingsViewModel.upgradeToPremium(activity) },
                onDismiss = { showTaskSummaryGraphDialog = false }
            )
        }
        if (showAchievementBadgesDialog) {
            PremiumFeatureDialog(
                feature = "Achievement Badges",
                description = "Earn badges for completing tasks and reaching milestones to stay motivated.",
                onUpgrade = { settingsViewModel.upgradeToPremium(activity) },
                onDismiss = { showAchievementBadgesDialog = false }
            )
        }
        if (showGraph && isPremium) {
            TaskSummaryGraph(
                viewModel = taskViewModel,
                aiRecommendationViewModel = aiRecommendationViewModel,
                onDismiss = { showGraph = false },
                onCloseClick = { showGraph = false },
                taskSummaryGraphManager = settingsViewModel.taskSummaryGraphManager
            )
        }
        if (showBadgeInfo && isPremium) {
            BadgeInfoDialog(
                currentBadge = currentBadge,
                tasksCompleted = tasksCompleted,
                nextBadgeInfo = Pair(Badge.valueOf(getNextBadge(currentBadge)), getTasksNeededForNextBadge(currentBadge, tasksCompleted)),
                onDismiss = { showBadgeInfo = false }
            )
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Tasks : Screen("tasks", "Tasks", Icons.Filled.List)
    object Notes : Screen("notes", "Notes", Icons.Filled.Note)
    object EditTask : Screen("edit_task", "Edit Task", Icons.Filled.Edit)
    object EditNote : Screen("edit_note", "Edit Note", Icons.Filled.Edit)
    object TaskDetail : Screen("task_detail", "Task Detail", Icons.Filled.Info)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun EditTaskScreen(taskId: Int, viewModel: TaskViewModel, onNavigateBack: () -> Unit) {
    // ... (unchanged)
}

@Composable
fun EditNoteScreen(viewModel: TaskViewModel, noteId: Int, onNavigateBack: () -> Unit) {
    // ... (unchanged)
}

@Composable
fun NoteDetailScreen(viewModel: TaskViewModel, noteId: Int, navController: NavController) {
    // ... (unchanged)
}
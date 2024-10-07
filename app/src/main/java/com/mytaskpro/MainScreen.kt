@file:OptIn(ExperimentalMaterial3Api::class)


package com.mytaskpro.ui

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
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import com.mytaskpro.ui.components.ThemeSelectionDialog
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

@ExperimentalMaterial3Api
@Composable
fun MainScreen(
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel
) {
    val navController = rememberNavController()
    var showThemeDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("MyTaskPro")
                            IconButton(onClick = { /* Navigate to profile screen */ }) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
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
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
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
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
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
            NavHost(
                navController = navController,
                startDestination = Screen.Tasks.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Tasks.route) {
                    TasksScreen(viewModel = taskViewModel)
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
                        onNavigateBack = { navController.popBackStack() }
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
                        onNavigateBack = { navController.popBackStack() }
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
                        navController = navController
                    )
                }
            }
        }

        // Theme icon in the top-right corner
        IconButton(
            onClick = { showThemeDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        ) {
            Icon(
                Icons.Default.Palette,
                contentDescription = "Change theme",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                themeViewModel.setTheme(theme)
                showThemeDialog = false
            }
        )
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Tasks : Screen("tasks", "Tasks", Icons.Filled.List)
    object Notes : Screen("notes", "Notes", Icons.Filled.Note)
    object EditTask : Screen("edit_task", "Edit Task", Icons.Filled.Edit)
    object EditNote : Screen("edit_note", "Edit Note", Icons.Filled.Edit)
}

@Composable
fun EditTaskScreen(taskId: Int, viewModel: TaskViewModel, onNavigateBack: () -> Unit) {
    // Implement your EditTaskScreen here
}

@Composable
fun EditNoteScreen(viewModel: TaskViewModel, noteId: Int, onNavigateBack: () -> Unit) {
    // Implement your EditNoteScreen here
}

@Composable
fun NoteDetailScreen(viewModel: TaskViewModel, noteId: Int, navController: NavController) {
    // Implement your NoteDetailScreen here
    // This screen should display the full details of a note, including images and PDFs
    // You can use the noteId to fetch the note details from the viewModel
}


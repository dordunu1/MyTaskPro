@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytaskpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mytaskpro.R
import com.mytaskpro.data.CategoryType
import com.mytaskpro.data.Task
import com.mytaskpro.ui.theme.*
import com.mytaskpro.viewmodel.TaskViewModel.FilterOption
import com.mytaskpro.viewmodel.TaskAdditionStatus
import com.mytaskpro.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.mytaskpro.viewmodel.TaskViewModel.SortOption
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.delay


fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

fun getColorForDueDate(dueDate: Date): Color {
    val now = Date()
    return when {
        dueDate.before(now) -> VibrantPink
        dueDate.time - now.time < 24 * 60 * 60 * 1000 -> VibrantOrange
        else -> VibrantGreen
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Int) -> Unit,
    onEditTask: (Int) -> Unit
) {
    val taskAdditionStatus by viewModel.taskAdditionStatus.collectAsState()
    val tasks by viewModel.filteredAndSortedTasks.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val completedTaskCount by viewModel.completedTaskCount.collectAsState(initial = 0)
    val showConfetti by viewModel.showConfetti.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showCategorySelection by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryType?>(null) }

    val customCategories by viewModel.customCategories.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(2000) // 2 seconds delay, adjust as needed
            viewModel.hideConfetti()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("✅ Tasks") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCategorySelection = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VibrantBlue.copy(alpha = 0.05f),
                                VibrantPurple.copy(alpha = 0.05f)
                            )
                        )
                    )
            ) {
                FilterAndSortBar(
                    filterOption = filterOption,
                    sortOption = sortOption,
                    completedTaskCount = completedTaskCount,
                    onFilterChanged = viewModel::updateFilterOption,
                    onSortChanged = viewModel::updateSortOption,
                    customCategories = customCategories
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = tasks,
                        key = { task -> task.id }
                    ) { task ->
                        TaskItem(
                            task = task,
                            viewModel = viewModel,
                            onEditTask = { editingTask = it },
                            onTaskClick = onTaskClick
                        )
                    }
                }
            }
        }
        if (showConfetti) {
            ConfettiAnimation(
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showCategorySelection) {
        val customCategories by viewModel.customCategories.collectAsState()
        CategorySelectionDialog(
            onDismiss = { showCategorySelection = false },
            onCategorySelected = { category ->
                showCategorySelection = false
                showAddTaskDialog = true
                selectedCategory = category
            },
            onNewCategoryCreated = { newCategoryName ->
                viewModel.createCustomCategory(newCategoryName)
            },
            customCategories = customCategories
        )
    }

    val currentCategory = selectedCategory
    if (showAddTaskDialog && currentCategory != null) {
        AddTaskDialog(
            category = currentCategory,
            onDismiss = {
                showAddTaskDialog = false
                selectedCategory = null
            },
            onTaskAdded = { title, description, dueDate, reminderTime, notifyOnDueDate, repetitiveSettings ->
                viewModel.addTask(
                    title = title,
                    description = description,
                    category = currentCategory,
                    dueDate = dueDate,
                    reminderTime = reminderTime,
                    notifyOnDueDate = notifyOnDueDate,
                    repetitiveSettings = repetitiveSettings
                )
                showAddTaskDialog = false
                selectedCategory = null
            }
        )
    }

    LaunchedEffect(editingTask) {
        editingTask?.let { task ->
            onEditTask(task.id)
            editingTask = null
        }
    }
    LaunchedEffect(taskAdditionStatus) {
        when (taskAdditionStatus) {
            TaskAdditionStatus.Success -> {
                snackbarHostState.showSnackbar("Task added successfully")
                viewModel.resetTaskAdditionStatus()
            }
            TaskAdditionStatus.Error -> {
                snackbarHostState.showSnackbar("Failed to add task")
                viewModel.resetTaskAdditionStatus()
            }
            TaskAdditionStatus.DuplicateTitle -> {
                snackbarHostState.showSnackbar("A task with this title already exists")
                viewModel.resetTaskAdditionStatus()
            }
            else -> {}
        }
    }
}


@Composable
fun TaskItem(
    task: Task,
    viewModel: TaskViewModel,
    onTaskClick: (Int) -> Unit,
    onEditTask: (Task) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onTaskClick(task.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.snoozeCount > 0)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(task.category.color), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = getColorForDueDate(task.dueDate),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "Due: ${formatDate(task.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                if (task.reminderTime != null) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Reminder set",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { isCompleted ->
                        viewModel.updateTaskCompletion(task.id, isCompleted)
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        if (task.snoozeCount > 0) {
                            viewModel.undoSnooze(task.id)
                        } else {
                            viewModel.toggleSnoozeOptions(task.id)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_snooze_custom),
                        contentDescription = if (task.snoozeCount > 0) "Undo Snooze" else "Snooze",
                        tint = if (task.snoozeCount > 0) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (task.showSnoozeOptions && task.snoozeCount == 0) {
                SnoozeOptions(task.id, viewModel)
            }
        }
    }


    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SnoozeOptions(taskId: Int, viewModel: TaskViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SnoozeButton("5m", 5, taskId, viewModel)
        SnoozeButton("15m", 15, taskId, viewModel)
        SnoozeButton("1h", 60, taskId, viewModel)
        SnoozeButton("1d", 1440, taskId, viewModel)
    }
}

@Composable
fun SnoozeButton(label: String, durationInMinutes: Int, taskId: Int, viewModel: TaskViewModel) {
    Button(
        onClick = { viewModel.snoozeTask(taskId, durationInMinutes.toLong() * 60 * 1000) },
        modifier = Modifier.padding(4.dp)
    ) {
        Text(label)
    }
}

@Composable
fun FilterAndSortBar(
    filterOption: FilterOption,
    sortOption: SortOption,
    completedTaskCount: Int,
    onFilterChanged: (FilterOption) -> Unit,
    onSortChanged: (SortOption) -> Unit,
    customCategories: List<CategoryType>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilterDropdown(
            selectedOption = filterOption,
            onOptionSelected = onFilterChanged,
            completedTaskCount = completedTaskCount,
            customCategories = customCategories
        )
        SortDropdown(
            selectedOption = sortOption,
            onOptionSelected = onSortChanged
        )
    }
}

@Composable
fun FilterDropdown(
    selectedOption: FilterOption,
    onOptionSelected: (FilterOption) -> Unit,
    completedTaskCount: Int,
    customCategories: List<CategoryType>
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(contentColor = VibrantBlue)
        ) {
            Text("Filter: ${
                when (selectedOption) {
                    is FilterOption.All -> "All"
                    is FilterOption.Category -> selectedOption.category.displayName
                    is FilterOption.Completed -> "Completed"
                    is FilterOption.CustomCategory -> selectedOption.category.displayName
                    else -> "Unknown"
                }
            }")
            Icon(Icons.Default.ArrowDropDown, "Expand")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onOptionSelected(FilterOption.All)
                    expanded = false
                }
            )
            CategoryType.values().forEach { category ->
                if (category.type != "COMPLETED") {
                    DropdownMenuItem(
                        text = { Text(category.displayName) },
                        onClick = {
                            onOptionSelected(FilterOption.Category(category))
                            expanded = false
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(category.color), CircleShape)
                            )
                        }
                    )
                }
            }
            customCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onOptionSelected(FilterOption.CustomCategory(category))
                        expanded = false
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(category.color), CircleShape)
                        )
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Completed ($completedTaskCount)") },
                onClick = {
                    onOptionSelected(FilterOption.Completed)
                    expanded = false
                },
                leadingIcon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                }
            )
        }
    }
}

@Composable
fun SortDropdown(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(contentColor = VibrantGreen)
        ) {
            Text("Sort: ${selectedOption.displayName}")
            Icon(Icons.Default.ArrowDropDown, "Expand")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
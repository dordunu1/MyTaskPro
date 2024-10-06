@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytaskpro.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.mytaskpro.viewmodel.SortOption
import com.mytaskpro.viewmodel.TaskAdditionStatus
import com.mytaskpro.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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

@Composable
fun TasksScreen(viewModel: TaskViewModel) {
    val taskAdditionStatus by viewModel.taskAdditionStatus.collectAsState()
    val tasks by viewModel.filteredAndSortedTasks.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showCategorySelection by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryType?>(null) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isScrolling by remember { mutableStateOf(false) }
    var lastScrollPosition by remember { mutableStateOf(0f) }

    remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                isScrolling = true
                lastScrollPosition = initialVelocity
                return initialVelocity
            }
        }
    }

    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            while (isScrolling) {
                lazyListState.scrollBy(lastScrollPosition * 0.98f)
                lastScrollPosition *= 0.98f
                if (abs(lastScrollPosition) < 0.01f) {
                    isScrolling = false
                }
                delay(16) // Approximately 60 FPS
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("✅ Tasks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = White,
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
                onFilterChanged = viewModel::updateFilterOption,
                onSortChanged = viewModel::updateSortOption
            )

            // Replace the existing LazyColumn with this new one
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(
                    items = tasks,
                    key = { task -> task.id }
                ) { task ->
                    TaskItem(
                        task = task,
                        viewModel = viewModel,
                        onEditTask = { editingTask = it }
                    )
                }
            }
        }
    }

    // Handle dialogs and other UI elements
    if (showCategorySelection) {
        CategorySelectionDialog(
            onDismiss = { showCategorySelection = false },
            onCategorySelected = { category ->
                showCategorySelection = false
                showAddTaskDialog = true
                selectedCategory = category
            }
        )
    }

    if (showAddTaskDialog && selectedCategory != null) {
        AddTaskDialog(
            category = selectedCategory!!,
            onDismiss = {
                showAddTaskDialog = false
                selectedCategory = null
            },
            onTaskAdded = { title, description, dueDate, reminderTime ->
                viewModel.addTask(title, description, selectedCategory!!, dueDate, reminderTime)
                showAddTaskDialog = false
                selectedCategory = null
            }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onTaskEdited = { updatedTask ->
                viewModel.updateTask(
                    updatedTask.id,
                    updatedTask.title,
                    updatedTask.description,
                    updatedTask.category,
                    updatedTask.dueDate,
                    updatedTask.reminderTime
                )
                editingTask = null
            }
        )
    }

    LaunchedEffect(taskAdditionStatus) {
        when (taskAdditionStatus) {
            is TaskAdditionStatus.Success -> {
                snackbarHostState.showSnackbar("Task added successfully")
                viewModel.resetTaskAdditionStatus()
            }
            is TaskAdditionStatus.DuplicateTitle -> {
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
    onEditTask: (Task) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium),
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
                Icon(
                    task.category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = getColorForDueDate(task.dueDate),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Due: ${formatDate(task.dueDate)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { isCompleted ->
                            viewModel.updateTaskCompletion(task.id, isCompleted)
                        }
                    )
                    IconButton(onClick = { onEditTask(task) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Task")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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
                IconButton(
                    onClick = { showDeleteConfirmation = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = Color.Red
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
fun TaskDetails(task: Task) {
    Column {
        Text(
            text = task.description,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Due: ${formatDate(task.dueDate)}",
            style = MaterialTheme.typography.bodySmall
        )
        if (task.reminderTime != null) {
            Text(
                text = "Reminder: ${formatDate(task.reminderTime)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TaskHeader(
    task: Task,
    viewModel: TaskViewModel,
    onEditTask: (Task) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                task.category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                color = getColorForDueDate(task.dueDate),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.reminderTime != null) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Reminder set",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { onEditTask(task) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Task"
                )
            }
        }
    }
    if (task.snoozeCount > 0) {
        Text(
            text = "Snoozed ${task.snoozeCount}x",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Red,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SnoozeOptions(taskId: Int, viewModel: TaskViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SnoozeButton("5m", TimeUnit.MINUTES.toMillis(5), taskId, viewModel)
        SnoozeButton("15m", TimeUnit.MINUTES.toMillis(15), taskId, viewModel)
        SnoozeButton("1h", TimeUnit.HOURS.toMillis(1), taskId, viewModel)
        SnoozeButton("1d", TimeUnit.DAYS.toMillis(1), taskId, viewModel)
    }
}

@Composable
fun SnoozeButton(label: String, duration: Long, taskId: Int, viewModel: TaskViewModel) {
    Button(
        onClick = { viewModel.snoozeTask(taskId, duration) },
        modifier = Modifier.padding(4.dp)
    ) {
        Text(label)
    }
}

@Composable
fun FilterAndSortBar(
    filterOption: CategoryType?,
    sortOption: SortOption,
    onFilterChanged: (CategoryType?) -> Unit,
    onSortChanged: (SortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilterDropdown(
            selectedOption = filterOption,
            onOptionSelected = onFilterChanged
        )
        SortDropdown(
            selectedOption = sortOption,
            onOptionSelected = onSortChanged
        )
    }
}

@Composable
fun FilterDropdown(
    selectedOption: CategoryType?,
    onOptionSelected: (CategoryType?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(contentColor = VibrantBlue)
        ) {
            Text("Filter: ${selectedOption?.displayName ?: "All"}")
            Icon(Icons.Default.ArrowDropDown, "Expand")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onOptionSelected(null)
                    expanded = false
                }
            )
            CategoryType.values().forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onOptionSelected(category)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(category.icon, contentDescription = null)
                    }
                )
            }
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


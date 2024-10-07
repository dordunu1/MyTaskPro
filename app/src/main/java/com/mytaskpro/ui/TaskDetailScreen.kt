package com.mytaskpro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.CategoryType
import com.mytaskpro.data.RepetitiveTaskSettings
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.ui.theme.CustomDatePickerDialog
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Date



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Int,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    isEditing: Boolean
) {
    var editing by remember { mutableStateOf(isEditing) }
    val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    task?.let { currentTask ->
        var title by remember { mutableStateOf(currentTask.title) }
        var description by remember { mutableStateOf(currentTask.description) }
        var category by remember { mutableStateOf(currentTask.category) }
        var dueDate by remember { mutableStateOf(currentTask.dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) }
        var dueTime by remember { mutableStateOf(currentTask.dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()) }
        var isReminderSet by remember { mutableStateOf(currentTask.reminderTime != null) }
        var reminderDate by remember { mutableStateOf(currentTask.reminderTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()) }
        var reminderTime by remember { mutableStateOf(currentTask.reminderTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: LocalTime.now()) }
        var notifyOnDueDate by remember { mutableStateOf(currentTask.notifyOnDueDate) }
        var isCompleted by remember { mutableStateOf(currentTask.isCompleted) }

        var showDueDatePicker by remember { mutableStateOf(false) }
        var showDueTimePicker by remember { mutableStateOf(false) }
        var showReminderDatePicker by remember { mutableStateOf(false) }
        var showReminderTimePicker by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (editing) "Edit Task" else "Task Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!editing) {
                            IconButton(onClick = { editing = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Task")
                            }
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategorySelectionDropdown(
                        selectedCategory = category,
                        onCategorySelected = { category = it }
                    )
                } else {
                    Text(currentTask.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(currentTask.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Category: ${currentTask.category.name}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Due Date: ${dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                        Text("Due Time: ${dueTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                    }
                    if (editing) {
                        Row {
                            IconButton(onClick = { showDueDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Change due date")
                            }
                            IconButton(onClick = { showDueTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Change due time")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = notifyOnDueDate,
                        onCheckedChange = { if (editing) notifyOnDueDate = it }
                    )
                    Text("Notify on due date")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isReminderSet,
                        onCheckedChange = { if (editing) isReminderSet = it }
                    )
                    Text("Set Reminder")
                }
                if (isReminderSet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reminder Date: ${reminderDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                            Text("Reminder Time: ${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                        }
                        if (editing) {
                            Row {
                                IconButton(onClick = { showReminderDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Change reminder date")
                                }
                                IconButton(onClick = { showReminderTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Change reminder time")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Repetitive Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                RepetitiveSettingsDisplay(currentTask.repetitiveSettings)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Task completed?", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { newIsCompleted ->
                            isCompleted = newIsCompleted
                            viewModel.updateTaskCompletion(currentTask.id, newIsCompleted)
                        }
                    )
                }
                if (currentTask.snoozeCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Snoozed ${currentTask.snoozeCount} time(s)", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { viewModel.undoSnooze(currentTask.id) }) {
                        Text("Undo Snooze")
                    }
                }
                if (editing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateTask(
                                    taskId = currentTask.id,
                                    title = title,
                                    description = description,
                                    category = category,
                                    dueDate = Date.from(dueDate.atTime(dueTime).atZone(ZoneId.systemDefault()).toInstant()),
                                    reminderTime = if (isReminderSet) Date.from(reminderDate.atTime(reminderTime).atZone(ZoneId.systemDefault()).toInstant()) else null,
                                    notifyOnDueDate = notifyOnDueDate,
                                    repetitiveSettings = currentTask.repetitiveSettings // Keep the original repetitive settings
                                )
                                editing = false
                            }
                        ) {
                            Text("Save Changes")
                        }
                        OutlinedButton(onClick = { editing = false }) {
                            Text("Cancel")
                        }
                    }
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
                            viewModel.deleteTask(taskId)
                            showDeleteConfirmation = false
                            onNavigateBack()
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

        if (showDueDatePicker) {
            CustomDatePickerDialog(
                initialDate = dueDate,
                onDateSelected = {
                    dueDate = it
                    showDueDatePicker = false
                },
                onDismiss = { showDueDatePicker = false }
            )
        }

        if (showDueTimePicker) {
            StableTimePickerDialog(
                onDismissRequest = { showDueTimePicker = false },
                onTimeSelected = {
                    dueTime = it
                    showDueTimePicker = false
                }
            )
        }

        if (showReminderDatePicker) {
            CustomDatePickerDialog(
                initialDate = reminderDate,
                onDateSelected = {
                    reminderDate = it
                    showReminderDatePicker = false
                },
                onDismiss = { showReminderDatePicker = false }
            )
        }

        if (showReminderTimePicker) {
            StableTimePickerDialog(
                onDismissRequest = { showReminderTimePicker = false },
                onTimeSelected = {
                    reminderTime = it
                    showReminderTimePicker = false
                }
            )
        }
    }
}

@Composable
fun RepetitiveSettingsDisplay(repetitiveSettings: RepetitiveTaskSettings?) {
    repetitiveSettings?.let { settings ->
        Text("Repetition: ${settings.type.name}", style = MaterialTheme.typography.bodyMedium)
        when (settings.type) {
            RepetitionType.ONE_TIME -> {
                Text("One-time task", style = MaterialTheme.typography.bodySmall)
            }
            RepetitionType.DAILY -> {
                Text("Repeats every ${settings.interval} day(s)", style = MaterialTheme.typography.bodySmall)
            }
            RepetitionType.WEEKDAYS -> {
                Text("Repeats on weekdays", style = MaterialTheme.typography.bodySmall)
            }
            RepetitionType.WEEKLY -> {
                Text("Repeats every ${settings.interval} week(s) on: ${settings.weekDays.joinToString(", ") { getDayOfWeek(it + 1) }}", style = MaterialTheme.typography.bodySmall)
            }
            RepetitionType.MONTHLY -> {
                if (settings.monthDay != null) {
                    Text("Repeats every ${settings.interval} month(s) on day ${settings.monthDay}", style = MaterialTheme.typography.bodySmall)
                } else if (settings.monthWeek != null && settings.monthWeekDay != null) {
                    Text("Repeats every ${settings.interval} month(s) on the ${getOrdinal(settings.monthWeek)} ${getDayOfWeek(settings.monthWeekDay)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            RepetitionType.YEARLY -> {
                Text("Repeats every ${settings.interval} year(s)", style = MaterialTheme.typography.bodySmall)
            }
        }
        when (settings.endOption) {
            EndOption.NEVER -> Text("Repeats indefinitely", style = MaterialTheme.typography.bodySmall)
            EndOption.BY_DATE -> Text("Until: ${settings.endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)}", style = MaterialTheme.typography.bodySmall)
            EndOption.AFTER_OCCURRENCES -> Text("For ${settings.endOccurrences} occurrences", style = MaterialTheme.typography.bodySmall)
        }
    } ?: Text("No repetitive settings", style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun CategorySelectionDropdown(
    selectedCategory: CategoryType,
    onCategorySelected: (CategoryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedCategory.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand category dropdown"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            CategoryType.values().forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
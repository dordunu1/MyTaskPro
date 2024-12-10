package com.mytaskpro.ui

import RepetitiveSettingsDialog
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
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Date
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.mytaskpro.data.TaskPriority

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
        var showRepetitiveSettingsDialog by remember { mutableStateOf(false) }
        var selectedPriority by remember { mutableStateOf(currentTask.priority) }
        var expandedPriority by remember { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedPriority.name,
                        onValueChange = {},
                        label = { Text("Priority") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(currentTask.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(currentTask.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Category: ${currentTask.category.displayName}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Priority: ${currentTask.priority.name}", style = MaterialTheme.typography.bodyMedium)
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
                RepetitiveSettingsDisplay(
                    repetitiveSettings = currentTask.repetitiveSettings,
                    isEditing = editing,
                    onEditClick = {
                        showRepetitiveSettingsDialog = true
                    }
                )
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
                                    repetitiveSettings = currentTask.repetitiveSettings,
                                    priority = selectedPriority
                                )
                                editing = false
                            }
                        ) {
                            Text(text = "Save Changes")
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
            TaskDetailDatePickerDialog(
                onDismissRequest = { showDueDatePicker = false },
                onDateSelected = { selectedDate ->
                    dueDate = selectedDate
                    showDueDatePicker = false
                },
                initialDate = dueDate
            )
        }

        if (showDueTimePicker) {
            TaskDetailTimePickerDialog(
                onDismissRequest = { showDueTimePicker = false },
                onTimeSelected = { selectedTime ->
                    dueTime = selectedTime
                    showDueTimePicker = false
                },
                initialTime = dueTime
            )
        }

        if (showReminderDatePicker) {
            MyDatePickerDialog(
                onDismissRequest = { showReminderDatePicker = false },
                onDateSelected = { selectedDate ->
                    reminderDate = selectedDate
                    showReminderDatePicker = false
                },
                initialDate = reminderDate
            )
        }

        if (showReminderTimePicker) {
            MyTimePickerDialog(
                onDismissRequest = { showReminderTimePicker = false },
                onTimeSelected = { selectedTime ->
                    reminderTime = selectedTime
                    showReminderTimePicker = false
                },
                initialTime = reminderTime
            )
        }

        if (showRepetitiveSettingsDialog) {
            RepetitiveSettingsDialog(
                currentSettings = currentTask.repetitiveSettings,
                onDismiss = { showRepetitiveSettingsDialog = false },
                onSave = { newSettings ->
                    viewModel.updateTaskRepetitiveSettings(currentTask.id, newSettings)
                    showRepetitiveSettingsDialog = false
                }
            )
        }
    }
}

@Composable
fun RepetitiveSettingsDisplay(
    repetitiveSettings: RepetitiveTaskSettings?,
    onEditClick: () -> Unit,
    isEditing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                        Text("Repeats on weekdays (Mon, Tue, Wed, Thu, Fri)", style = MaterialTheme.typography.bodySmall)
                    }
                    RepetitionType.WEEKLY -> {
                        val weekDays = settings.weekDays.map { getDayOfWeekInitial(it + 1) }.joinToString(", ")
                        Text("Repeats every ${settings.interval} week(s) on: $weekDays", style = MaterialTheme.typography.bodySmall)
                    }
                    RepetitionType.MONTHLY -> {
                        if (settings.monthDay != null) {
                            Text("Repeats every ${settings.interval} month(s) on day ${settings.monthDay}", style = MaterialTheme.typography.bodySmall)
                        } else if (settings.monthWeek != null && settings.monthWeekDay != null) {
                            Text("Repeats every ${settings.interval} month(s) on the ${getOrdinal(settings.monthWeek)} ${getDayOfWeekInitial(settings.monthWeekDay)}", style = MaterialTheme.typography.bodySmall)
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
        if (isEditing) {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit repetitive settings")
            }
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onDateSelected(selectedDate)
                }
                onDismissRequest()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailTimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                onDismissRequest()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

// Helper functions
fun getOrdinal(n: Int): String {
    return when (n) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        4 -> "4th"
        5 -> "5th"
        6 -> "6th"
        7 -> "7th"
        else -> "1st"
    }
}

fun getDayOfWeekInitial(n: Int): String {
    return when (n) {
        1 -> "Sun"
        2 -> "Mon"
        3 -> "Tue"
        4 -> "Wed"
        5 -> "Thu"
        6 -> "Fri"
        7 -> "Sat"
        else -> "Invalid"
    }
}
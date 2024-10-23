package com.mytaskpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.CategoryType
import com.mytaskpro.ui.theme.*
import java.util.Date
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ArrowDropDown
import com.mytaskpro.data.RepetitiveTaskSettings
import com.mytaskpro.data.TaskPriority
import com.mytaskpro.utils.TimeUtils
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    category: CategoryType,
    onDismiss: () -> Unit,
    onTaskAdded: (String, String, Date, Date?, Boolean, RepetitiveTaskSettings?, TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var dueTime by remember { mutableStateOf(LocalTime.now()) }
    var reminderDate by remember { mutableStateOf(LocalDate.now()) }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }
    var isReminderSet by remember { mutableStateOf(false) }
    var notifyOnDueDate by remember { mutableStateOf(true) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showRepetitiveTaskDialog by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var repetitiveTaskSettings by remember { mutableStateOf<RepetitiveTaskSettings?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Task ✅", color = VibrantBlue)
                IconButton(onClick = { showRepetitiveTaskDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Set as repeating task",
                        tint = if (repetitiveTaskSettings != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(category.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Category: ${category.displayName}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📅 Set Due Date: $dueDate")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showDueTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⏰ Set Due Time: ${TimeUtils.formatTime(dueTime)}")
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = notifyOnDueDate,
                        onCheckedChange = { notifyOnDueDate = it }
                    )
                    Text("Notify on due date")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isReminderSet,
                        onCheckedChange = { isReminderSet = it }
                    )
                    Text("Set Reminder")
                }
                if (isReminderSet) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showReminderDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔔 Set Reminder Date: $reminderDate")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showReminderTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⏰ Set Reminder Time: ${TimeUtils.formatTime(reminderTime)}")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    Button(
                        onClick = { showPriorityDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Priority: $priority")
                        Icon(Icons.Default.ArrowDropDown, "Show priority options")
                    }
                    DropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false }
                    ) {
                        TaskPriority.values().forEach { priorityOption ->
                            DropdownMenuItem(
                                text = { Text(priorityOption.name) },
                                onClick = {
                                    priority = priorityOption
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dueDatetime = Date.from(
                        dueDate.atTime(dueTime).atZone(ZoneId.systemDefault()).toInstant()
                    )
                    val reminderDatetime = if (isReminderSet) {
                        Date.from(
                            reminderDate.atTime(reminderTime).atZone(ZoneId.systemDefault())
                                .toInstant()
                        )
                    } else null
                    onTaskAdded(title, description, dueDatetime, reminderDatetime, notifyOnDueDate, repetitiveTaskSettings, priority)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDueDatePicker) {
        MyDatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            onDateSelected = { selectedDate ->
                dueDate = selectedDate
                showDueDatePicker = false
            },
            initialDate = dueDate
        )
    }

    if (showDueTimePicker) {
        MyTimePickerDialog(
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

    if (showRepetitiveTaskDialog) {
        RepetitiveTaskDialog(
            isVisible = showRepetitiveTaskDialog,
            onDismiss = { showRepetitiveTaskDialog = false },
            onConfirm = { settings ->
                repetitiveTaskSettings = settings
                showRepetitiveTaskDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    var showingPicker by remember { mutableStateOf(true) }

    if (showingPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute
        )

        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showingPicker = false
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
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
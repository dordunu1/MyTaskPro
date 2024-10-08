package com.mytaskpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.CategoryType
import com.mytaskpro.ui.theme.*
import java.util.Date
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.runtime.LaunchedEffect
import com.mytaskpro.ui.StableTimePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.graphics.vector.ImageVector
import com.mytaskpro.data.RepetitiveTaskSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    category: CategoryType,
    onDismiss: () -> Unit,
    onTaskAdded: (String, String, Date, Date?, Boolean, RepetitiveTaskSettings?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var dueTime by remember { mutableStateOf(LocalTime.now()) }
    var reminderDate by remember { mutableStateOf(LocalDate.now()) }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }
    var isReminderSet by remember { mutableStateOf(false) }
    var notifyOnDueDate by remember { mutableStateOf(true) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showRepetitiveTaskDialog by remember { mutableStateOf(false) }
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
                    when (category) {
                        is CategoryType.Custom -> {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(category.color)
                            )
                        }
                        else -> {
                            Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
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
                    Text("⏰ Set Due Time: $dueTime")
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
                        Text("⏰ Set Reminder Time: $reminderTime")
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
                    onTaskAdded(title, description, dueDatetime, reminderDatetime, notifyOnDueDate, repetitiveTaskSettings)
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
            onTimeSelected = { selectedTime ->
                dueTime = selectedTime
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
            onTimeSelected = { selectedTime ->
                reminderTime = selectedTime
                showReminderTimePicker = false
            }
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
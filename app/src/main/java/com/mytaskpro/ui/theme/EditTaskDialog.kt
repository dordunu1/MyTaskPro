package com.mytaskpro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.Task
import com.mytaskpro.data.CategoryType
import java.util.Date
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.mytaskpro.ui.theme.CustomDatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onTaskEdited: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var category by remember { mutableStateOf(task.category) }
    var dueDate by remember { mutableStateOf(task.dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) }
    var dueTime by remember { mutableStateOf(task.dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()) }
    var isReminderSet by remember { mutableStateOf(task.reminderTime != null) }
    var reminderDate by remember { mutableStateOf(task.reminderTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()) }
    var reminderTime by remember { mutableStateOf(task.reminderTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: LocalTime.now()) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // Make the content scrollable
            ) {
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
                        .height(100.dp), // Limit the height of the description field
                    maxLines = 5 // Limit the number of visible lines
                )
                Spacer(modifier = Modifier.height(8.dp))
                CategorySelectionDropdown(
                    selectedCategory = category,
                    onCategorySelected = { category = it }
                )
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
                    Row {
                        IconButton(onClick = { showDueDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Change due date")
                        }
                        IconButton(onClick = { showDueTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Change due time")
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isReminderSet,
                        onCheckedChange = { isReminderSet = it }
                    )
                    Text("Set Reminder")
                }
                if (isReminderSet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reminder Date: ${reminderDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                            Text("Reminder Time: ${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                        }
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedTask = task.copy(
                        title = title,
                        description = description,
                        category = category,
                        dueDate = Date.from(dueDate.atTime(dueTime).atZone(ZoneId.systemDefault()).toInstant()),
                        reminderTime = if (isReminderSet) Date.from(reminderDate.atTime(reminderTime).atZone(ZoneId.systemDefault()).toInstant()) else null
                    )
                    onTaskEdited(updatedTask)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
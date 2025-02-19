package com.mytaskpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.RepetitiveTaskSettings
import java.time.LocalDate
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset

enum class RepetitionType {
    ONE_TIME, DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY
}

enum class EndOption {
    NEVER, BY_DATE, AFTER_OCCURRENCES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepetitiveTaskDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (RepetitiveTaskSettings) -> Unit
) {
    var settings by remember { mutableStateOf(RepetitiveTaskSettings()) }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set as Repeating Task") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("Repeat:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    RepetitionTypeSelector(
                        selectedType = settings.type,
                        onTypeSelected = { settings = settings.copy(type = it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    when (settings.type) {
                        RepetitionType.ONE_TIME -> {}
                        RepetitionType.DAILY -> DailyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                        RepetitionType.WEEKDAYS -> WeekdaysRepetitionOption(settings, onSettingsChanged = { settings = it })
                        RepetitionType.WEEKLY -> WeeklyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                        RepetitionType.MONTHLY -> MonthlyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                        RepetitionType.YEARLY -> YearlyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                    }
                    if (settings.type != RepetitionType.ONE_TIME) {
                        Spacer(modifier = Modifier.height(16.dp))
                        EndOptionsSelector(settings, onSettingsChanged = { settings = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(settings) }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RepetitionTypeSelector(
    selectedType: RepetitionType,
    onTypeSelected: (RepetitionType) -> Unit
) {
    Column {
        RepetitionType.values().forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = type == selectedType,
                    onClick = { onTypeSelected(type) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(type.name.replace("_", " ").capitalize())
            }
        }
    }
}

@Composable
fun IntervalSelector(
    intervalType: String,
    currentInterval: Int,
    onIntervalChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Repeats Every:")
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text("$currentInterval $intervalType(s)")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                (1..30).forEach { number ->
                    DropdownMenuItem(
                        text = { Text("$number $intervalType(s)") },
                        onClick = {
                            onIntervalChanged(number)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun VerticalPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .height(120.dp)
            .width(60.dp)
    ) {
        LazyColumn {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clickable { onItemSelected(item) }
                        .background(if (item == selectedItem) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item == selectedItem) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DailyRepetitionOptions(settings: RepetitiveTaskSettings, onSettingsChanged: (RepetitiveTaskSettings) -> Unit) {
    IntervalSelector("Day", settings.interval) { onSettingsChanged(settings.copy(interval = it)) }
}

@Composable
fun WeekdaysRepetitionOption(settings: RepetitiveTaskSettings, onSettingsChanged: (RepetitiveTaskSettings) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Repeat on weekdays",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = settings.weekDays.size == 5, // Assuming 5 weekdays
            onCheckedChange = { isChecked ->
                val newWeekDays = if (isChecked) {
                    listOf(0, 1, 2, 3, 4) // Monday to Friday
                } else {
                    emptyList()
                }
                onSettingsChanged(settings.copy(weekDays = newWeekDays))
            }
        )
    }
}

@Composable
fun DayToggleButton(
    day: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun WeeklyRepetitionOptions(settings: RepetitiveTaskSettings, onSettingsChanged: (RepetitiveTaskSettings) -> Unit) {
    IntervalSelector("Week", settings.interval) { onSettingsChanged(settings.copy(interval = it)) }

    Spacer(modifier = Modifier.height(8.dp))

    Text("Repeat on:", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day ->
            DayToggleButton(
                day = day,
                isSelected = index in settings.weekDays,
                onToggle = {
                    val newWeekDays = settings.weekDays.toMutableList()
                    if (index in newWeekDays) newWeekDays.remove(index) else newWeekDays.add(index)
                    onSettingsChanged(settings.copy(weekDays = newWeekDays))
                }
            )
        }
    }
}

@Composable
fun VerticalPickerString(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .height(120.dp)
            .width(120.dp)
    ) {
        LazyColumn {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clickable { onItemSelected(item) }
                        .background(if (item == selectedItem) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item == selectedItem) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
@Composable
fun MonthlyRepetitionOptions(settings: RepetitiveTaskSettings, onSettingsChanged: (RepetitiveTaskSettings) -> Unit) {
    IntervalSelector("Month", settings.interval) { onSettingsChanged(settings.copy(interval = it)) }
    Spacer(modifier = Modifier.height(16.dp))
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Monthly - Day of Month")
            Spacer(modifier = Modifier.width(8.dp))
            VerticalPicker(
                items = (1..31).toList(),
                selectedItem = settings.monthDay ?: 1,
                onItemSelected = { onSettingsChanged(settings.copy(monthDay = it)) }
            )
        }
    }
}

@Composable
fun YearlyRepetitionOptions(settings: RepetitiveTaskSettings, onSettingsChanged: (RepetitiveTaskSettings) -> Unit) {
    IntervalSelector("Year", settings.interval) { onSettingsChanged(settings.copy(interval = it)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndOptionsSelector(settings: RepetitiveTaskSettings, onSettingsChanged: (RepetitiveTaskSettings) -> Unit) {
    var endOption by remember { mutableStateOf(settings.endOption) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column {
        Text("Ends:", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = endOption == EndOption.NEVER,
                onClick = {
                    endOption = EndOption.NEVER
                    onSettingsChanged(RepetitiveTaskSettings.fromLocalDate(
                        type = settings.type,
                        interval = settings.interval,
                        weekDays = settings.weekDays,
                        monthDay = settings.monthDay,
                        monthWeek = settings.monthWeek,
                        monthWeekDay = settings.monthWeekDay,
                        endOption = EndOption.NEVER,
                        endDate = null,
                        endOccurrences = null
                    ))
                }
            )
            Text("Never")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = endOption == EndOption.BY_DATE,
                onClick = {
                    endOption = EndOption.BY_DATE
                    onSettingsChanged(RepetitiveTaskSettings.fromLocalDate(
                        type = settings.type,
                        interval = settings.interval,
                        weekDays = settings.weekDays,
                        monthDay = settings.monthDay,
                        monthWeek = settings.monthWeek,
                        monthWeekDay = settings.monthWeekDay,
                        endOption = EndOption.BY_DATE,
                        endDate = LocalDate.now(),
                        endOccurrences = null
                    ))
                    showDatePicker = true
                }
            )
            Text("By Date")
            if (endOption == EndOption.BY_DATE) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showDatePicker = true }) {
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
                    Text(settings.endDate?.format(dateFormatter) ?: "Select Date")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = endOption == EndOption.AFTER_OCCURRENCES,
                onClick = {
                    endOption = EndOption.AFTER_OCCURRENCES
                    onSettingsChanged(RepetitiveTaskSettings.fromLocalDate(
                        type = settings.type,
                        interval = settings.interval,
                        weekDays = settings.weekDays,
                        monthDay = settings.monthDay,
                        monthWeek = settings.monthWeek,
                        monthWeekDay = settings.monthWeekDay,
                        endOption = EndOption.AFTER_OCCURRENCES,
                        endDate = null,
                        endOccurrences = 1
                    ))
                }
            )
            Text("After Occurrences")
            if (endOption == EndOption.AFTER_OCCURRENCES) {
                Spacer(modifier = Modifier.width(8.dp))
                VerticalPicker(
                    items = (1..100).toList(),
                    selectedItem = settings.endOccurrences ?: 1,
                    onItemSelected = { occurrences ->
                        onSettingsChanged(RepetitiveTaskSettings.fromLocalDate(
                            type = settings.type,
                            interval = settings.interval,
                            weekDays = settings.weekDays,
                            monthDay = settings.monthDay,
                            monthWeek = settings.monthWeek,
                            monthWeekDay = settings.monthWeekDay,
                            endOption = EndOption.AFTER_OCCURRENCES,
                            endDate = null,
                            endOccurrences = occurrences
                        ))
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        val currentTimeMillis = System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = settings.endDate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                ?: currentTimeMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onSettingsChanged(RepetitiveTaskSettings.fromLocalDate(
                            type = settings.type,
                            interval = settings.interval,
                            weekDays = settings.weekDays,
                            monthDay = settings.monthDay,
                            monthWeek = settings.monthWeek,
                            monthWeekDay = settings.monthWeekDay,
                            endOption = EndOption.BY_DATE,
                            endDate = selectedDate,
                            endOccurrences = null
                        ))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

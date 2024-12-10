package com.mytaskpro.ui

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.mytaskpro.viewmodel.TaskViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mytaskpro.data.Task
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.mytaskpro.data.CategoryType
import com.mytaskpro.data.TaskSummary
import com.mytaskpro.managers.TaskSummaryGraphManager
import com.mytaskpro.ui.components.AIRecommendationSection
import com.mytaskpro.ui.viewmodel.AIRecommendationViewModel
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.compose.component.textComponent

@Composable
fun TaskSummaryGraph(
    viewModel: TaskViewModel,
    aiRecommendationViewModel: AIRecommendationViewModel,
    taskSummaryGraphManager: TaskSummaryGraphManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit
) {
    val isEnabled by taskSummaryGraphManager.isEnabled.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState(initial = emptyMap())

    LaunchedEffect(Unit) {
        viewModel.refreshUpcomingTasks()
    }

    if (isEnabled) {
        var selectedTimeFrame by remember { mutableStateOf(TimeFrame.WEEKLY) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 40.dp, bottom = 80.dp),  // Increased top padding
                state = rememberLazyListState()
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .padding(vertical = 8.dp),  // Added vertical padding
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),  // Adjusted height
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Task Summary",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = onCloseClick) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Task Summary"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            TimeFrameSelector(
                                selectedTimeFrame = selectedTimeFrame,
                                onTimeFrameSelected = { selectedTimeFrame = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MainGraph(
                        viewModel = viewModel,
                        timeFrame = selectedTimeFrame,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    KeyMetricsDashboard(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailedAnalytics(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        UpcomingTasksSection(
                            upcomingTasks = upcomingTasks,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AIRecommendationSection(
                        viewModel = aiRecommendationViewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Task Summary Graph is currently disabled. Enable it in settings to view your task analytics.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier  // Added
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TimeFrame.values().forEach { timeFrame ->
            Button(
                onClick = { onTimeFrameSelected(timeFrame) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timeFrame == selectedTimeFrame)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface,
                    contentColor = if (timeFrame == selectedTimeFrame)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = timeFrame.displayName,
                    color = if (timeFrame == selectedTimeFrame)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MainGraph(
    viewModel: TaskViewModel,
    timeFrame: TimeFrame,
    modifier: Modifier = Modifier
) {
    val completionData by viewModel.getCompletionData(timeFrame).collectAsState(initial = emptyList())
    val categoryData by viewModel.getCategoryCompletionData(timeFrame).collectAsState(initial = emptyMap())
    var selectedPoint by remember { mutableStateOf<Pair<Int, List<Task>>?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val chartEntryModel = entryModelOf(
                completionData.mapIndexed { index, (completed, total) ->
                    FloatEntry(x = index.toFloat(), y = if (total > 0) (completed.toFloat() / total) * 100 else 0f)
                },
                *categoryData.map { (category, data) ->
                    data.mapIndexed { index, (completed, total) ->
                        FloatEntry(x = index.toFloat(), y = if (total > 0) (completed.toFloat() / total) * 100 else 0f)
                    }
                }.toTypedArray()
            )

            val chartColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.error
            )

            Chart(
                chart = lineChart(
                    lines = categoryData.keys.mapIndexed { index, category ->
                        LineChart.LineSpec(
                            lineColor = chartColors[index % chartColors.size].toArgb(),
                            lineBackgroundShader = verticalGradient(
                                colors = arrayOf(
                                    chartColors[index % chartColors.size].copy(alpha = 0.5f),
                                    chartColors[index % chartColors.size].copy(alpha = 0.1f)
                                )
                            )
                        )
                    } + LineChart.LineSpec(
                        lineColor = MaterialTheme.colorScheme.primary.toArgb(),
                        lineBackgroundShader = verticalGradient(
                            colors = arrayOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    )
                ),
                model = chartEntryModel,
                startAxis = startAxis(
                    label = axisLabelComponent(color = MaterialTheme.colorScheme.onSurface),
                    title = "Completion Rate (%)",
                    titleComponent = axisTitleComponent(color = MaterialTheme.colorScheme.onSurface),
                    valueFormatter = { value, _ -> "${value.toInt()}%" },
                    guideline = null
                ),
                bottomAxis = bottomAxis(
                    label = axisLabelComponent(color = MaterialTheme.colorScheme.onSurface),
                    title = when (timeFrame) {
                        TimeFrame.DAILY -> "Days"
                        TimeFrame.WEEKLY -> "Weeks"
                        TimeFrame.MONTHLY -> "Months"
                    },
                    titleComponent = axisTitleComponent(color = MaterialTheme.colorScheme.onSurface),
                    valueFormatter = { value, _ ->
                        when (timeFrame) {
                            TimeFrame.DAILY -> "Day ${value.toInt() + 1}"
                            TimeFrame.WEEKLY -> "Week ${value.toInt() + 1}"
                            TimeFrame.MONTHLY -> "Month ${value.toInt() + 1}"
                        }
                    },
                    guideline = null
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }

    selectedPoint?.let { (index, tasks) ->
        TaskDetailsDialog(
            tasks = tasks,
            onDismiss = { selectedPoint = null }
        )
    }
}

@Composable
fun TaskDetailsDialog(tasks: List<Task>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tasks for Selected Point") },
        text = {
            LazyColumn {
                items(tasks) { task ->
                    TaskItem(task)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun TaskItem(task: Task) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = task.title, fontWeight = FontWeight.Bold)
        Text(text = "Category: ${task.category}")
        Text(text = "Due: ${task.dueDate}")
        Text(text = "Reminder: ${task.reminderTime ?: "None"}")
        Text(text = "Status: ${if (task.isCompleted) "Completed" else "Pending"}")
    }
}

@Composable
fun axisLabelComponent(color: Color) = textComponent(
    color = color,
    textSize = 12.sp,
    typeface = Typeface.SANS_SERIF
)

@Composable
fun axisTitleComponent(color: Color) = textComponent(
    color = color,
    textSize = 14.sp,
    typeface = Typeface.DEFAULT_BOLD
)

@Composable
fun KeyMetricsDashboard(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier  // Added
) {
    var productivityScore by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var overdueTasks by remember { mutableStateOf(0) }
    var avgTurnaround by remember { mutableStateOf(0f) }

    LaunchedEffect(key1 = viewModel) {
        productivityScore = viewModel.getProductivityScore()
        currentStreak = viewModel.getCurrentStreak()
        overdueTasks = viewModel.getOverdueTasks()
        avgTurnaround = viewModel.getAverageTurnaround()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Key Metrics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    MetricItem(
                        title = "Productivity Score",
                        value = "$productivityScore",
                        suffix = "%",
                        tooltipText = "Percentage of completed tasks out of total tasks. Higher score indicates better productivity."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MetricItem(
                        title = "Current Streak",
                        value = "$currentStreak",
                        suffix = "days",
                        tooltipText = "Number of consecutive days with at least one completed task. Longer streaks show consistent productivity."
                    )
                }
                Column {
                    MetricItem(
                        title = "Overdue Tasks",
                        value = "$overdueTasks",
                        suffix = "",
                        tooltipText = "Number of tasks that have passed their due date without being completed. Fewer overdue tasks indicate better task management."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MetricItem(
                        title = "Avg. Turnaround",
                        value = String.format("%.1f", avgTurnaround),
                        suffix = "days",
                        tooltipText = "Average time taken to complete tasks from due date to completion. Negative values indicate tasks completed before the due date."
                    )
                }
            }
        }
    }
}


@Composable
fun MetricItem(title: String, value: String, suffix: String, tooltipText: String) {
    var isTooltipVisible by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .clickable { isTooltipVisible = !isTooltipVisible }
            .padding(8.dp)
            .width(140.dp)  // Increased width to accommodate longer text
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (suffix.isNotEmpty()) "$value $suffix" else value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        AnimatedVisibility(
            visible = isTooltipVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = tooltipText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
@Composable
fun MetricTooltip(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DetailedAnalytics(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier  // Added
) {
    val analyticsData by viewModel.getBasicAnalytics().collectAsState(initial = null)
    val categoryColors = remember { getCategoryColors() }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Detailed Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        analyticsData?.let { data ->
            AnalyticsCard(
                title = "Summary",
                content = data.summary,
                categoryColors = categoryColors,
                onItemClick = { expandedSection = it }
            )

            AnalyticsCard(
                title = "Task Categories",
                content = data.categoryBreakdown,
                categoryColors = categoryColors,
                onItemClick = { category -> expandedSection = "Category: $category" }
            )

            AnalyticsCard(
                title = "Recent Activity",
                content = data.recentActivity,
                categoryColors = categoryColors,
                onItemClick = { } // Empty lambda to make it non-clickable
            )
        }
    }

    // Show dialog with expanded details when a section is clicked
    expandedSection?.let { section ->
        val tasks = when {
            section.startsWith("Category:") -> analyticsData?.detailedTasks?.get(section)
            else -> analyticsData?.detailedTasks?.get(section)
        } ?: emptyList()
        ExpandedDetailsDialog(
            title = section,
            tasks = tasks,
            onDismiss = { expandedSection = null }
        )
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    content: Any,
    categoryColors: Map<String, Color>,
    onItemClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            when (content) {
                is Map<*, *> -> {
                    if (title == "Task Categories") {
                        content.forEach { (key, value) ->
                            CategoryItem(
                                category = key.toString(),
                                count = (value as? Pair<*, *>)?.first.toString(),
                                color = (value as? Pair<*, *>)?.second as? Color ?: Color.Gray,
                                onClick = { onItemClick(key.toString()) }
                            )
                        }
                    } else {
                        content.forEach { (key, value) ->
                            AnalyticsItem(
                                label = key.toString(),
                                value = value.toString(),
                                onClick = { onItemClick(key.toString()) }
                            )
                        }
                    }
                }
                is List<*> -> {
                    content.forEach { item ->
                        AnalyticsItem(
                            label = item.toString(),
                            value = "",
                            onClick = { onItemClick(title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: String,
    count: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnalyticsItem(
    label: String,
    value: String,
    color: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            color?.let {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color = it, shape = CircleShape)
                        .padding(end = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExpandedDetailsDialog(
    title: String,
    tasks: List<TaskSummary>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(tasks) { task ->
                    TaskItem(task)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TaskItem(task: TaskSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(task.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(task.categoryColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(task.category, style = MaterialTheme.typography.bodySmall)
        }
        Text("Due: ${task.dueDate}", style = MaterialTheme.typography.bodySmall)
        Text("Reminder: ${task.reminder}", style = MaterialTheme.typography.bodySmall)
        Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall, color = if (task.status == "Completed") Color.Green else Color.Red)
    }
}

fun getCategoryColors(): Map<String, Color> {
    return CategoryType.values().associate { it.displayName to Color(it.color) }
}


enum class TimeFrame(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}
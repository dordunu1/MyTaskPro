package com.mytaskpro.data

import androidx.compose.ui.graphics.Color

data class AnalyticsData(
    val summary: Map<String, String>,
    val categoryBreakdown: Map<String, Pair<Int, Color>>, // Changed to include color
    val recentActivity: List<String>,
    val detailedTasks: Map<String, List<TaskSummary>>
)

data class TaskSummary(
    val title: String,
    val category: String,
    val description: String,
    val categoryColor: Color,
    val dueDate: String,
    val reminder: String,
    val status: String
)
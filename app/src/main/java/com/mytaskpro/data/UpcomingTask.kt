package com.mytaskpro.data

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

data class UpcomingTask(
    val id: Long,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val dueTime: LocalTime,
    val category: CategoryType,
    val isCompleted: Boolean = false
) {
    val categoryColor: Color
        get() = Color(category.color)
}
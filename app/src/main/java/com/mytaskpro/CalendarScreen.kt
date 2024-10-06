package com.mytaskpro.ui


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import com.mytaskpro.viewmodel.TaskViewModel

@Composable
fun CalendarScreen(viewModel: TaskViewModel) {
    val currentMonth = remember { YearMonth.now() }
    val startDate = remember { currentMonth.minusMonths(100).atStartOfMonth() }
    val endDate = remember { currentMonth.plusMonths(100).atEndOfMonth() }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberWeekCalendarState(
        startDate = startDate,
        endDate = endDate,
        firstVisibleWeekDate = currentMonth.atStartOfMonth(),
        firstDayOfWeek = firstDayOfWeek
    )

    Column {
        WeekCalendar(
            state = state,
            dayContent = { day ->
                Day(day.date)
            }
        )
        // Here you would add a list of tasks for the selected date
    }
}

@Composable
fun Day(date: LocalDate) {
    Text(text = date.dayOfMonth.toString())
}
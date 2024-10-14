package com.mytaskpro.ai

import com.mytaskpro.data.Task
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import com.mytaskpro.data.UserAction
import com.mytaskpro.data.UserActionType

class TaskAnalyzer @Inject constructor() {
    fun analyzeCompletionPatterns(tasks: List<Task>): Map<String, Boolean> {
        val completedTasks = tasks.filter { it.isCompleted }
        val morningCompletions = completedTasks.count { task ->
            val calendar = Calendar.getInstance().apply { time = task.completionDate ?: Date() }
            calendar.get(Calendar.HOUR_OF_DAY) < 12
        }
        return mapOf("morningProductivity" to (morningCompletions > completedTasks.size / 2))
    }

    fun getOverdueTasks(tasks: List<Task>): List<Task> {
        val now = Date()
        return tasks.filter { !it.isCompleted && it.dueDate.before(now) }
    }

    fun getFrequentlyPostponedTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { it.snoozeCount > 3 }
    }

    fun getRecentlyCreatedTasks(tasks: List<Task>): List<Task> {
        val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        return tasks.filter { it.creationDate.after(oneWeekAgo) }
    }

    fun getRecentlyCompletedTasks(tasks: List<Task>): List<Task> {
        val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        return tasks.filter { it.isCompleted && (it.completionDate?.after(oneWeekAgo) ?: false) }
    }

    fun analyzeTaskCategories(tasks: List<Task>): Map<String, List<Task>> {
        return tasks.groupBy { it.category.toString() }
    }

    fun analyzeProductiveTimeSlots(tasks: List<Task>, recentActions: List<UserAction>): List<Pair<Int, Int>> {
        val completionHours = tasks.filter { it.isCompleted }
            .mapNotNull { it.completionDate }
            .map { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) }

        val actionHours = recentActions
            .filter { it.type == UserActionType.COMPLETE_TASK }
            .map { Calendar.getInstance().apply { time = it.timestamp }.get(Calendar.HOUR_OF_DAY) }

        val allHours = completionHours + actionHours
        val hourCounts = allHours.groupBy { it }.mapValues { it.value.size }

        val productiveHours = hourCounts.filter { it.value > hourCounts.values.average() }
            .keys.sorted()

        return findConsecutiveRanges(productiveHours)
    }

    private fun findConsecutiveRanges(hours: List<Int>): List<Pair<Int, Int>> {
        if (hours.isEmpty()) return emptyList()

        val ranges = mutableListOf<Pair<Int, Int>>()
        var start = hours.first()
        var end = start

        for (i in 1 until hours.size) {
            if (hours[i] == end + 1) {
                end = hours[i]
            } else {
                ranges.add(Pair(start, end))
                start = hours[i]
                end = start
            }
        }
        ranges.add(Pair(start, end))

        return ranges.map { Pair(it.first, max(it.second, it.first + 1)) }
    }
}
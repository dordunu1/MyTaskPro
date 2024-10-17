package com.mytaskpro.ai

import com.mytaskpro.data.Task
import com.mytaskpro.data.UserAction
import com.mytaskpro.data.UserActionType
import com.mytaskpro.data.CategoryType
import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import javax.inject.Inject

class AIRecommendationEngine @Inject constructor(private val taskAnalyzer: TaskAnalyzer) {
    fun generateRecommendations(
        tasks: List<Task>,
        recentActions: List<UserAction>
    ): List<RecommendationModel> {
        val recommendations = mutableListOf<RecommendationModel>()

        val completionPatterns = taskAnalyzer.analyzeCompletionPatterns(tasks)
        val overdueTasks = taskAnalyzer.getOverdueTasks(tasks)
        val frequentlySnoozedTasks = taskAnalyzer.getFrequentlySnoozedTasks(tasks)
        val recentlyCreatedTasks = taskAnalyzer.getRecentlyCreatedTasks(tasks)
        val recentlyCompletedTasks = taskAnalyzer.getRecentlyCompletedTasks(tasks)
        val taskCategories = taskAnalyzer.analyzeTaskCategories(tasks)
        val productiveTimeSlots = taskAnalyzer.analyzeProductiveTimeSlots(tasks, recentActions)

        addProductivityRecommendation(recommendations, completionPatterns)
        addOverdueTasksRecommendation(recommendations, overdueTasks)
        addFrequentlySnoozedTasksRecommendation(recommendations, frequentlySnoozedTasks)
        addRecentActivityRecommendation(recommendations, recentActions)
        addTaskCreationPatternRecommendation(recommendations, recentlyCreatedTasks)
        addCompletionRateRecommendation(recommendations, tasks, recentlyCompletedTasks)
        addCategoryBasedRecommendation(recommendations, taskCategories)
        addTimeBasedRecommendation(recommendations, productiveTimeSlots)

        return recommendations
    }

    private fun getCategoryDisplayName(category: Any): String {
        return when (category) {
            is CategoryType -> category.displayName
            is String -> category
            else -> category.toString()
        }
    }

    private fun addProductivityRecommendation(
        recommendations: MutableList<RecommendationModel>,
        completionPatterns: Map<String, Boolean>
    ) {
        if (completionPatterns["morningProductivity"] == true) {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.RESCHEDULE,
                    "You seem to be more productive in the mornings. Consider scheduling important tasks earlier in the day.",
                    emptyList()
                )
            )
        } else {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.RESCHEDULE,
                    "Consider analyzing your most productive times of day and schedule important tasks accordingly.",
                    emptyList()
                )
            )
        }
    }

    private fun addOverdueTasksRecommendation(
        recommendations: MutableList<RecommendationModel>,
        overdueTasks: List<Task>
    ) {
        if (overdueTasks.isNotEmpty()) {
            recommendations.add(RecommendationModel(
                RecommendationType.PRIORITIZE,
                "You have ${overdueTasks.size} overdue tasks. Consider prioritizing these tasks.",
                overdueTasks.map { it.id }
            ))
        } else {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.PRIORITIZE,
                    "Great job keeping up with your tasks! Remember to prioritize tasks to maintain your productivity.",
                    emptyList()
                )
            )
        }
    }

    private fun addFrequentlySnoozedTasksRecommendation(
        recommendations: MutableList<RecommendationModel>,
        frequentlySnoozedTasks: List<Task>
    ) {
        if (frequentlySnoozedTasks.isNotEmpty()) {
            val taskCount = frequentlySnoozedTasks.size
            val taskWord = if (taskCount == 1) "task" else "tasks"
            recommendations.add(RecommendationModel(
                RecommendationType.BREAK_DOWN,
                "You've snoozed $taskCount $taskWord frequently. Consider breaking these down into smaller, more manageable subtasks:",
                frequentlySnoozedTasks.map { it.id }
            ))
        } else {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.BREAK_DOWN,
                    "You're doing well at tackling tasks without snoozing. If you ever struggle with a task, try breaking it down into smaller steps.",
                    emptyList()
                )
            )
        }
    }


    private fun addRecentActivityRecommendation(
        recommendations: MutableList<RecommendationModel>,
        recentActions: List<UserAction>
    ) {
        val creationCount = recentActions.count { it.type == UserActionType.CREATE_TASK }
        val completionCount = recentActions.count { it.type == UserActionType.COMPLETE_TASK }

        when {
            creationCount > completionCount -> recommendations.add(
                RecommendationModel(
                    RecommendationType.FOCUS,
                    "You've been creating more tasks than completing them recently. Focus on finishing some existing tasks before adding new ones.",
                    emptyList()
                )
            )

            completionCount > creationCount * 2 -> recommendations.add(
                RecommendationModel(
                    RecommendationType.MOTIVATION,
                    "Great job on completing tasks! You're making excellent progress. Keep up the momentum!",
                    emptyList()
                )
            )

            else -> recommendations.add(
                RecommendationModel(
                    RecommendationType.FOCUS,
                    "Your task creation and completion rates are balanced. Keep up the good work!",
                    emptyList()
                )
            )
        }
    }

    private fun addTaskCreationPatternRecommendation(
        recommendations: MutableList<RecommendationModel>,
        recentlyCreatedTasks: List<Task>
    ) {
        val categoryCounts = recentlyCreatedTasks.groupBy { getCategoryDisplayName(it.category) }
            .mapValues { it.value.size }
        val mostCommonCategory = categoryCounts.maxByOrNull { it.value }

        if (mostCommonCategory != null) {
            val (categoryName, count) = mostCommonCategory
            recommendations.add(
                RecommendationModel(
                    RecommendationType.INSIGHT,
                    "You've been creating a lot of tasks in the '$categoryName' category ($count tasks). Consider focusing on this area or balancing with other categories.",
                    emptyList()
                )
            )
        } else {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.INSIGHT,
                    "You're creating a good mix of tasks across different categories. Keep up the diverse approach!",
                    emptyList()
                )
            )
        }
    }

    private fun addCompletionRateRecommendation(
        recommendations: MutableList<RecommendationModel>,
        allTasks: List<Task>,
        recentlyCompletedTasks: List<Task>
    ) {
        val completionRate =
            if (allTasks.isNotEmpty()) recentlyCompletedTasks.size.toFloat() / allTasks.size else 0f
        val completionPercentage = (completionRate * 100).toInt()

        when {
            completionRate > 0.7 -> recommendations.add(
                RecommendationModel(
                    RecommendationType.MOTIVATION,
                    "Impressive completion rate of $completionPercentage%! You're doing an excellent job managing your tasks.",
                    emptyList()
                )
            )

            completionRate < 0.3 -> recommendations.add(
                RecommendationModel(
                    RecommendationType.STRATEGY,
                    "Your task completion rate is $completionPercentage%. Try setting smaller, more achievable goals to build momentum.",
                    emptyList()
                )
            )

            else -> recommendations.add(
                RecommendationModel(
                    RecommendationType.STRATEGY,
                    "Your task completion rate is $completionPercentage%. You're doing well, but there's room for improvement. Keep pushing yourself!",
                    emptyList()
                )
            )
        }
    }

    private fun addCategoryBasedRecommendation(
        recommendations: MutableList<RecommendationModel>,
        taskCategories: Map<String, List<Task>>
    ) {
        val neglectedCategory = taskCategories.minByOrNull { it.value.size }
        if (neglectedCategory != null) {
            val (categoryName, tasks) = neglectedCategory
            if (tasks.isNotEmpty()) {
                recommendations.add(RecommendationModel(
                    RecommendationType.BALANCE,
                    "You have fewer tasks in the '${getCategoryDisplayName(categoryName)}' category (${tasks.size} tasks). Consider if this area needs more attention.",
                    tasks.map { it.id }
                ))
            } else {
                recommendations.add(
                    RecommendationModel(
                        RecommendationType.BALANCE,
                        "Your tasks are well-distributed across categories. Keep maintaining this balance!",
                        emptyList()
                    )
                )
            }
        } else {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.BALANCE,
                    "You don't have any tasks yet. Start by adding tasks in different categories to maintain a balanced approach.",
                    emptyList()
                )
            )
        }
    }

    private fun addTimeBasedRecommendation(
        recommendations: MutableList<RecommendationModel>,
        productiveTimeSlots: List<Pair<Int, Int>>
    ) {
        if (productiveTimeSlots.isNotEmpty()) {
            val (startHour, endHour) = productiveTimeSlots.first()
            recommendations.add(
                RecommendationModel(
                    RecommendationType.OPTIMIZE,
                    "You seem most productive between $startHour:00 and $endHour:00. Try scheduling important tasks during this time.",
                    emptyList()
                )
            )
        } else {
            recommendations.add(
                RecommendationModel(
                    RecommendationType.OPTIMIZE,
                    "We don't have enough data to determine your most productive time yet. Keep using the app, and we'll provide insights soon!",
                    emptyList()
                )
            )
        }
    }
}
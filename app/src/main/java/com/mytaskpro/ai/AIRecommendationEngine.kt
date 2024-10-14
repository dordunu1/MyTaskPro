package com.mytaskpro.ai

import com.mytaskpro.data.Task
import com.mytaskpro.data.UserAction
import com.mytaskpro.data.UserActionType
import com.mytaskpro.data.CategoryType
import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import javax.inject.Inject

class AIRecommendationEngine @Inject constructor(private val taskAnalyzer: TaskAnalyzer) {
    fun generateRecommendations(tasks: List<Task>, recentActions: List<UserAction>): List<RecommendationModel> {
        val recommendations = mutableListOf<RecommendationModel>()

        val completionPatterns = taskAnalyzer.analyzeCompletionPatterns(tasks)
        val overdueTasks = taskAnalyzer.getOverdueTasks(tasks)
        val frequentlyPostponedTasks = taskAnalyzer.getFrequentlyPostponedTasks(tasks)
        val recentlyCreatedTasks = taskAnalyzer.getRecentlyCreatedTasks(tasks)
        val recentlyCompletedTasks = taskAnalyzer.getRecentlyCompletedTasks(tasks)
        val taskCategories = taskAnalyzer.analyzeTaskCategories(tasks)
        val productiveTimeSlots = taskAnalyzer.analyzeProductiveTimeSlots(tasks, recentActions)

        addProductivityRecommendation(recommendations, completionPatterns)
        addOverdueTasksRecommendation(recommendations, overdueTasks)
        addPostponedTasksRecommendation(recommendations, frequentlyPostponedTasks)
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

    private fun addProductivityRecommendation(recommendations: MutableList<RecommendationModel>, completionPatterns: Map<String, Boolean>) {
        if (completionPatterns["morningProductivity"] == true) {
            recommendations.add(RecommendationModel(
                RecommendationType.RESCHEDULE,
                "You seem to be more productive in the mornings. Consider scheduling important tasks earlier in the day.",
                emptyList()
            ))
        }
    }

    private fun addOverdueTasksRecommendation(recommendations: MutableList<RecommendationModel>, overdueTasks: List<Task>) {
        if (overdueTasks.isNotEmpty()) {
            recommendations.add(RecommendationModel(
                RecommendationType.PRIORITIZE,
                "You have ${overdueTasks.size} overdue tasks. Consider prioritizing these tasks.",
                overdueTasks.map { it.id }
            ))
        }
    }

    private fun addPostponedTasksRecommendation(recommendations: MutableList<RecommendationModel>, frequentlyPostponedTasks: List<Task>) {
        if (frequentlyPostponedTasks.isNotEmpty()) {
            recommendations.add(RecommendationModel(
                RecommendationType.BREAK_DOWN,
                "Some tasks are frequently postponed. Try breaking them down into smaller, manageable subtasks.",
                frequentlyPostponedTasks.map { it.id }
            ))
        }
    }

    private fun addRecentActivityRecommendation(recommendations: MutableList<RecommendationModel>, recentActions: List<UserAction>) {
        val creationCount = recentActions.count { it.type == UserActionType.CREATE_TASK }
        val completionCount = recentActions.count { it.type == UserActionType.COMPLETE_TASK }

        if (creationCount > completionCount) {
            recommendations.add(RecommendationModel(
                RecommendationType.FOCUS,
                "You've been creating more tasks than completing them recently. Focus on finishing some existing tasks before adding new ones.",
                emptyList()
            ))
        } else if (completionCount > creationCount * 2) {
            recommendations.add(RecommendationModel(
                RecommendationType.MOTIVATION,
                "Great job on completing tasks! You're making excellent progress. Keep up the momentum!",
                emptyList()
            ))
        }
    }

    private fun addTaskCreationPatternRecommendation(recommendations: MutableList<RecommendationModel>, recentlyCreatedTasks: List<Task>) {
        val categoryCounts = recentlyCreatedTasks.groupBy { getCategoryDisplayName(it.category) }.mapValues { it.value.size }
        val mostCommonCategory = categoryCounts.maxByOrNull { it.value }

        mostCommonCategory?.let { (categoryName, count) ->
            recommendations.add(RecommendationModel(
                RecommendationType.INSIGHT,
                "You've been creating a lot of tasks in the '$categoryName' category ($count tasks). Consider focusing on this area or balancing with other categories.",
                emptyList()
            ))
        }
    }

    private fun addCompletionRateRecommendation(recommendations: MutableList<RecommendationModel>, allTasks: List<Task>, recentlyCompletedTasks: List<Task>) {
        val completionRate = recentlyCompletedTasks.size.toFloat() / allTasks.size
        when {
            completionRate > 0.7 -> recommendations.add(RecommendationModel(
                RecommendationType.MOTIVATION,
                "Impressive completion rate! You're doing an excellent job managing your tasks.",
                emptyList()
            ))
            completionRate < 0.3 -> recommendations.add(RecommendationModel(
                RecommendationType.STRATEGY,
                "Your task completion rate is a bit low. Try setting smaller, more achievable goals to build momentum.",
                emptyList()
            ))
        }
    }

    private fun addCategoryBasedRecommendation(recommendations: MutableList<RecommendationModel>, taskCategories: Map<String, List<Task>>) {
        val neglectedCategory = taskCategories.minByOrNull { it.value.size }
        neglectedCategory?.let { (categoryName, tasks) ->
            if (tasks.isNotEmpty()) {
                recommendations.add(RecommendationModel(
                    RecommendationType.BALANCE,
                    "You have fewer tasks in the '${getCategoryDisplayName(categoryName)}' category (${tasks.size} tasks). Consider if this area needs more attention.",
                    tasks.map { it.id }
                ))
            }
        }
    }

    private fun addTimeBasedRecommendation(recommendations: MutableList<RecommendationModel>, productiveTimeSlots: List<Pair<Int, Int>>) {
        if (productiveTimeSlots.isNotEmpty()) {
            val (startHour, endHour) = productiveTimeSlots.first()
            recommendations.add(RecommendationModel(
                RecommendationType.OPTIMIZE,
                "You seem most productive between $startHour:00 and $endHour:00. Try scheduling important tasks during this time.",
                emptyList()
            ))
        }
    }
}
package com.mytaskpro.ai

data class RecommendationModel(
    val type: RecommendationType,
    val description: String,
    val relatedTaskIds: List<Int>
)

enum class RecommendationType {
    RESCHEDULE,
    BREAK_DOWN,
    PRIORITIZE,
    DELEGATE,
    FOCUS,
    MOTIVATION,
    INSIGHT,
    STRATEGY,
    BALANCE,
    OPTIMIZE
}
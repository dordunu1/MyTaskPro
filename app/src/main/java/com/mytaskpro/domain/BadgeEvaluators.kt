package com.mytaskpro.domain

import com.mytaskpro.data.Badge
import com.mytaskpro.data.UserBadgeInfo
import javax.inject.Inject

class TaskCompletionBadgeEvaluator @Inject constructor() : BadgeEvaluator {
    override fun evaluate(currentBadge: Badge, tasksCompleted: Int): Badge {
        val newBadge = when {
            tasksCompleted >= 350 -> Badge.DIAMOND
            tasksCompleted >= 200 -> Badge.GOLD
            tasksCompleted >= 80 -> Badge.SILVER
            tasksCompleted >= 30 -> Badge.BRONZE
            else -> Badge.NONE
        }
        return if (newBadge.ordinal > currentBadge.ordinal) newBadge else currentBadge
    }
}


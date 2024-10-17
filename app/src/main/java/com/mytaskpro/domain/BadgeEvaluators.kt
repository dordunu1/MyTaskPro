package com.mytaskpro.domain

import com.mytaskpro.data.Badge
import com.mytaskpro.data.UserBadgeInfo
import javax.inject.Inject

class TaskCompletionBadgeEvaluator : BadgeEvaluator {
    override fun evaluate(tasksCompleted: Int): Badge {
        return when {
            tasksCompleted >= 1 -> Badge.BRONZE
            tasksCompleted >= 50 -> Badge.SILVER
            tasksCompleted >= 100 -> Badge.GOLD
            tasksCompleted >= 200 -> Badge.DIAMOND
            else -> Badge.NONE
        }
    }
}



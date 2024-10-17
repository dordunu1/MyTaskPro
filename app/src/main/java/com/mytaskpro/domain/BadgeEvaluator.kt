package com.mytaskpro.domain

import com.mytaskpro.data.Badge
import com.mytaskpro.data.UserBadgeInfo

interface BadgeEvaluator {
    fun evaluate(tasksCompleted: Int): Badge
}
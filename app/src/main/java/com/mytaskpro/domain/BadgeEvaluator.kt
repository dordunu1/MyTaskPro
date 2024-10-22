package com.mytaskpro.domain

import com.mytaskpro.data.Badge
import com.mytaskpro.data.UserBadgeInfo

interface BadgeEvaluator {
    fun evaluate(currentBadge: Badge, tasksCompleted: Int): Badge
}
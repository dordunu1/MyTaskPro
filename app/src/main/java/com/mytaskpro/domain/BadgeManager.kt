package com.mytaskpro.domain

import android.util.Log
import com.mytaskpro.data.Badge
import com.mytaskpro.data.UserBadgeInfo
import com.mytaskpro.repository.BadgeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

class BadgeManager @Inject constructor(
    private val badgeRepository: BadgeRepository,
    private val badgeEvaluator: BadgeEvaluator
) {

    class TaskCompletionBadgeEvaluator : BadgeEvaluator {
        override fun evaluate(currentBadge: Badge, tasksCompleted: Int): Badge {
            val newBadge = when {
                tasksCompleted >= 350 -> Badge.DIAMOND
                tasksCompleted >= 200 -> Badge.GOLD
                tasksCompleted >= 80 -> Badge.SILVER
                tasksCompleted >= 30 -> Badge.BRONZE  // Changed from 30 to 2 for testing
                else -> Badge.NONE
            }
            return if (newBadge.ordinal > currentBadge.ordinal) newBadge else currentBadge
        }
    }

    suspend fun evaluateUserBadge(userId: String) {
        val currentBadgeInfo = badgeRepository.getBadgeInfoForUser(userId).first() ?: return
        val tasksCompleted = currentBadgeInfo.tasksCompleted
        val newBadge = badgeEvaluator.evaluate(currentBadgeInfo.currentBadge, tasksCompleted)
        Log.d(
            "BadgeManager",
            "Evaluating badge: Tasks completed: $tasksCompleted, Current badge: ${currentBadgeInfo.currentBadge}, New badge: $newBadge"
        )

        if (newBadge != currentBadgeInfo.currentBadge) {
            badgeRepository.updateUserBadge(userId, newBadge)
            Log.d(
                "BadgeManager",
                "Badge updated from ${currentBadgeInfo.currentBadge} to $newBadge"
            )
        }
    }

    suspend fun incrementTasksCompleted(userId: String) {
        badgeRepository.incrementTasksCompleted(userId)
        evaluateUserBadge(userId)
    }
}
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
    private val badgeEvaluator: TaskCompletionBadgeEvaluator
) {

    fun evaluateBadge(completedTaskCount: Int): Badge {
        return when {
            completedTaskCount >= 350 -> Badge.DIAMOND
            completedTaskCount >= 200 -> Badge.GOLD
            completedTaskCount >= 80 -> Badge.SILVER
            completedTaskCount >= 30 -> Badge.BRONZE
            else -> Badge.NONE
        }
    }

    suspend fun evaluateUserBadge(userId: String) {
        val currentBadgeInfo = badgeRepository.getBadgeInfoForUser(userId).first() ?: return
        val tasksCompleted = currentBadgeInfo.tasksCompleted
        val newBadge = badgeEvaluator.evaluate(tasksCompleted)
        Log.d(
            "BadgeManager",
            "Evaluating badge: Tasks completed: $tasksCompleted, New badge: $newBadge"
        )

        Log.d(
            "BadgeManager",
            "Evaluating badge: Tasks completed: $tasksCompleted, New badge: $newBadge"
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
package com.mytaskpro.repository

import com.mytaskpro.data.Badge
import com.mytaskpro.data.BadgeDao
import com.mytaskpro.data.UserBadgeInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BadgeRepository @Inject constructor(
    private val badgeDao: BadgeDao
) {
    fun getBadgeInfoForUser(userId: String): Flow<UserBadgeInfo?> {
        return badgeDao.getBadgeInfoForUser(userId)
    }

    suspend fun insertOrUpdateBadgeInfo(badgeInfo: UserBadgeInfo) {
        badgeDao.insertOrUpdateBadgeInfo(badgeInfo)
    }

    suspend fun updateUserBadge(userId: String, newBadge: Badge) {
        badgeDao.updateUserBadge(userId, newBadge)
    }

    suspend fun incrementTasksCompleted(userId: String) {
        badgeDao.incrementTasksCompleted(userId)
    }
}

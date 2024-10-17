package com.mytaskpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBadgeInfo(badgeInfo: UserBadgeInfo)

    @Query("SELECT * FROM user_badge_info WHERE userId = :userId")
    fun getBadgeInfoForUser(userId: String): Flow<UserBadgeInfo?>

    @Query("UPDATE user_badge_info SET currentBadge = :newBadge WHERE userId = :userId")
    suspend fun updateUserBadge(userId: String, newBadge: Badge)

    @Query("UPDATE user_badge_info SET tasksCompleted = tasksCompleted + 1 WHERE userId = :userId")
    suspend fun incrementTasksCompleted(userId: String)

    @Query("UPDATE user_badge_info SET streak = :newStreak WHERE userId = :userId")
    suspend fun updateStreak(userId: String, newStreak: Int)
}
package com.mytaskpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_badge_info")
data class UserBadgeInfo(
    @PrimaryKey val userId: String,
    val currentBadge: Badge = Badge.NONE,
    val tasksCompleted: Int = 0,
    val streak: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
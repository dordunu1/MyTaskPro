package com.mytaskpro.data

data class User(
    val id: String,
    val name: String,
    val email: String,
    val badgeInfo: UserBadgeInfo
)
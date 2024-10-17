package com.mytaskpro.data

enum class Badge(val displayName: String, val description: String) {
    NONE("No Badge", "Start completing tasks to earn your first badge!"),
    BRONZE("Bronze", "You're off to a great start!"),
    SILVER("Silver", "You're making excellent progress!"),
    GOLD("Gold", "You're a task management pro!"),
    DIAMOND("Diamond", "You're a true productivity master!")
}
package com.mytaskpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: CategoryType,
    val dueDate: Date,
    val reminderTime: Date?,
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false,
    val snoozeCount: Int = 0,
    val showSnoozeOptions: Boolean = false,
    val notifyOnDueDate: Boolean = true  // Add this line
)
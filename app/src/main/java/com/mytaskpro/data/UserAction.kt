package com.mytaskpro.data

import java.util.Date

data class UserAction(
    val type: UserActionType,
    val timestamp: Date,
    val taskId: Int? = null
)

enum class UserActionType {
    CREATE_TASK,
    COMPLETE_TASK,
    MODIFY_TASK,
    DELETE_TASK
}
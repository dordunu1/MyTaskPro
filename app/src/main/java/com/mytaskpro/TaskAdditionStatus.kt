package com.mytaskpro.viewmodel

sealed class TaskAdditionStatus {
    object Idle : TaskAdditionStatus()
    object Success : TaskAdditionStatus()
    object Error : TaskAdditionStatus()
    object DuplicateTitle : TaskAdditionStatus()
}
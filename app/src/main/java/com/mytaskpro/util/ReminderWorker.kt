package com.mytaskpro.util

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mytaskpro.MainActivity

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val taskId = inputData.getInt("taskId", -1)
        if (taskId == -1) return Result.failure()

        val taskTitle = inputData.getString("taskTitle") ?: return Result.failure()
        val taskDescription = inputData.getString("taskDescription") ?: ""

        // Create intents for notification actions
        val completeIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "COMPLETE_TASK"
            putExtra("taskId", taskId)
        }

        val snoozeIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "SNOOZE_REMINDER"
            putExtra("taskId", taskId)
        }

        NotificationHelper.showNotification(
            applicationContext,
            taskId,
            "Reminder: $taskTitle",
            taskDescription,
            completeIntent,
            snoozeIntent
        )

        return Result.success()
    }
}
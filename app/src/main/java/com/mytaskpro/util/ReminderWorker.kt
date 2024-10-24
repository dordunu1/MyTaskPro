package com.mytaskpro.util

import android.content.Context
import android.content.Intent
import android.util.Log
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
        val isReminder = inputData.getBoolean("isReminder", true)

        Log.d("ReminderWorker", "Processing notification for task $taskId, isReminder: $isReminder")

        // Create intents for notification actions
        val completeIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "COMPLETE_TASK"
            putExtra("taskId", taskId)
        }

        val snoozeIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "SNOOZE_REMINDER"
            putExtra("taskId", taskId)
        }

        val notificationTitle = if (isReminder) "Reminder: $taskTitle" else "Due Now: $taskTitle"
        val notificationContent = if (isReminder) {
            "It's time for your scheduled reminder."
        } else {
            "This task is now due."
        }

        NotificationHelper.showNotification(
            applicationContext,
            taskId,
            notificationTitle,
            "$notificationContent\n$taskDescription"
        )

        Log.d("ReminderWorker", "Notification shown for task $taskId")

        return Result.success()
    }
}
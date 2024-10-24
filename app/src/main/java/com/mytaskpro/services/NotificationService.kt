package com.mytaskpro.services // Replace with your actual package name

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mytaskpro.NotificationActionReceiver
import com.mytaskpro.R

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // ... (existing channel creation code)
    }

    fun showTaskNotification(taskId: Int, title: String, description: String) {
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.mytaskpro.COMPLETE_TASK"
            putExtra("TASK_ID", taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.mytaskpro.SNOOZE_TASK"
            putExtra("TASK_ID", taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId + 1000, // Use a different request code to avoid conflicts
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(R.drawable.ic_check, "Complete", completePendingIntent)
            .addAction(R.drawable.ic_snooze_custom, "Snooze", snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId, notification)
    }

    companion object {
        const val CHANNEL_ID = "task_notifications"
    }
}
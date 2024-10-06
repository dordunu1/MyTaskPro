package com.mytaskpro.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mytaskpro.R

object NotificationHelper {
    private const val CHANNEL_ID = "TaskReminderChannel"
    private const val CHANNEL_NAME = "Task Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for task reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        taskId: Int,
        title: String,
        content: String,
        completeIntent: Intent,
        snoozeIntent: Intent
    ) {
        Log.d("NotificationHelper", "Attempting to show notification for task $taskId")

        createNotificationChannel(context)  // Ensure channel is created

        if (!areNotificationsEnabled(context)) {
            Log.w("NotificationHelper", "Notifications are not enabled for the app")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val completePendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePendingIntent = PendingIntent.getActivity(
            context,
            taskId + 1, // Use a different request code to avoid overwriting
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Complete", completePendingIntent)
            .addAction(R.drawable.ic_snooze_custom, "Snooze", snoozePendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(taskId, builder.build())
            }
            Log.d("NotificationHelper", "Notification shown successfully for task $taskId")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing notification: ${e.message}", e)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
            }
        }
        context.startActivity(intent)
    }
}
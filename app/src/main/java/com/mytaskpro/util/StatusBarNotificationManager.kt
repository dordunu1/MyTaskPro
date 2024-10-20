package com.mytaskpro.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.mytaskpro.MainActivity
import com.mytaskpro.R
import com.mytaskpro.data.Task
import com.mytaskpro.data.TaskDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.content.res.Configuration
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*



class StatusBarNotificationManager @Inject constructor(
    private val context: Context,
    private val taskDao: TaskDao, // Inject TaskDao instead of a repository
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "quick_add_channel"
    private val notificationId = 1

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Quick Add Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Allows quick addition of tasks from the status bar"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun isInDarkMode(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    fun showQuickAddNotification(taskCountForToday: Int, tasks: List<Task>) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "SHOW_CATEGORY_SELECTION"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val contentTitle = when {
            taskCountForToday == 0 -> "No tasks today"
            taskCountForToday == 1 -> "1 Task"
            else -> "$taskCountForToday Tasks"
        }

        val contentText = "TODAY"

        val isDarkMode = isInDarkMode(context)
        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val dateFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

        val collapsedView = RemoteViews(context.packageName, R.layout.notification_quick_add_collapsed).apply {
            setTextViewText(R.id.title, contentTitle)
            setTextViewText(R.id.text, contentText)
            setTextColor(R.id.title, textColor)
            setTextColor(R.id.text, textColor)
            setOnClickPendingIntent(R.id.add_button, pendingIntent)
        }


        val expandedView = RemoteViews(context.packageName, R.layout.notification_quick_add_expanded).apply {
            setTextViewText(R.id.title, contentTitle)
            setTextViewText(R.id.text, contentText)
            setTextColor(R.id.title, textColor)
            setTextColor(R.id.text, textColor)
            setOnClickPendingIntent(R.id.add_button, pendingIntent)

            // Add tasks with due dates to the expanded view
            removeAllViews(R.id.task_list)
            tasks.take(5).forEachIndexed { index, task ->
                val taskView = RemoteViews(context.packageName, R.layout.notification_task_item)
                taskView.setTextViewText(R.id.task_title, "${index + 1}. ${task.title}")

                val formattedDate = dateFormatter.format(task.dueDate)

                taskView.setTextViewText(R.id.task_due_date, "Due: $formattedDate")
                taskView.setTextColor(R.id.task_title, textColor)
                taskView.setTextColor(R.id.task_due_date, textColor)
                addView(R.id.task_list, taskView)
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_add_task)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun updateQuickAddNotification(isEnabled: Boolean) {
        if (isEnabled) {
            CoroutineScope(Dispatchers.IO).launch {
                val todaysTasks = getTodaysTasks()
                val taskCount = todaysTasks.size
                withContext(Dispatchers.Main) {
                    showQuickAddNotification(taskCount, todaysTasks)
                }
            }
        } else {
            hideQuickAddNotification()
        }
    }

    private suspend fun getTodaysTasks(): List<Task> {
        val today = System.currentTimeMillis()
        return taskDao.getTasksForDate(today)
    }


    fun hideQuickAddNotification() {
        notificationManager.cancel(notificationId)
    }
}
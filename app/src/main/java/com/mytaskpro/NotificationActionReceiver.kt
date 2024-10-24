package com.mytaskpro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mytaskpro.data.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var taskDao: TaskDao

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "onReceive called with action: ${intent.action}")
        val taskId = intent.getIntExtra("TASK_ID", -1)
        Log.d("NotificationReceiver", "Task ID received: $taskId")

        if (taskId != -1) {
            when (intent.action) {
                "com.mytaskpro.COMPLETE_TASK" -> {
                    Log.d("NotificationReceiver", "Attempting to complete task: $taskId")
                    completeTask(taskId)
                }
                "com.mytaskpro.SNOOZE_TASK" -> {
                    Log.d("NotificationReceiver", "Attempting to snooze task: $taskId")
                    snoozeTask(context, taskId)
                }
                else -> {
                    Log.w("NotificationReceiver", "Unknown action received: ${intent.action}")
                }
            }
        } else {
            Log.e("NotificationReceiver", "Received intent without valid taskId")
        }
    }

    private fun completeTask(taskId: Int) {
        Log.d("NotificationReceiver", "completeTask called for task: $taskId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                taskDao.markTaskAsCompleted(taskId, Date())
                Log.d("NotificationReceiver", "Task $taskId marked as completed successfully")
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error completing task $taskId: ${e.message}", e)
            }
        }
    }

    private fun snoozeTask(context: Context, taskId: Int) {
        Log.d("NotificationReceiver", "snoozeTask called for task: $taskId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getTaskById(taskId) ?: throw Exception("Task not found")
                val snoozeDuration = 25 * 60 * 1000L // 25 minutes, change as needed
                val newTime = Date(System.currentTimeMillis() + snoozeDuration)

                val updatedTask = task.copy(
                    dueDate = newTime,
                    reminderTime = newTime,
                    isSnoozed = true,
                    snoozeCount = task.snoozeCount + 1
                )

                taskDao.updateTask(updatedTask)
                Log.d("NotificationReceiver", "Task $taskId snoozed successfully")

                // Notify the app to refresh UI and update widget
                val refreshIntent = Intent("com.mytaskpro.REFRESH_TASKS")
                context.sendBroadcast(refreshIntent)
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error snoozing task $taskId: ${e.message}", e)
            }
        }
    }
}
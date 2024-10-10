package com.mytaskpro.widget

import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mytaskpro.R
import com.mytaskpro.data.Task
import com.mytaskpro.data.TaskDao
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class TaskWidgetRemoteViewsFactory(
    private val context: Context,
    private val taskDao: TaskDao,
    private val appWidgetId: Int,
    currentTime: Long
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "TaskWidgetRemoteViewsFactory"
        private const val MAX_TASKS = 10
    }

    override fun onCreate() {
        // No initialization needed
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val currentDate = Date()
                tasks = taskDao.getUpcomingTasks(MAX_TASKS, currentDate)
                Log.d(TAG, "Fetched ${tasks.size} tasks for widget. Tasks: ${tasks.map { it.title }}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tasks for widget", e)
                tasks = emptyList()
            }
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
        coroutineScope.cancel()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= tasks.size) return null

        return try {
            val task = tasks[position]
            RemoteViews(context.packageName, R.layout.widget_task_item).apply {
                setTextViewText(R.id.widget_task_title, task.title)
                setTextViewText(R.id.widget_task_due_date, formatDateTime(task.dueDate))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view for task at position $position", e)
            null
        }
    }

    private fun formatDateTime(date: Date): String {
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
package com.mytaskpro.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mytaskpro.R
import com.mytaskpro.data.Task
import com.mytaskpro.data.TaskDao
import kotlinx.coroutines.runBlocking
import java.util.*

class TaskWidgetRemoteViewsFactory(
    private val context: Context,
    private val taskDao: TaskDao,
    private val appWidgetId: Int,
    private val maxTasks: Int,
    private val currentTime: Long
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()

    override fun onCreate() {
        // No initialization needed
    }

    override fun onDataSetChanged() {
        // This is called to refresh the data in the widget
        runBlocking {
            tasks = taskDao.getUpcomingTasks(maxTasks, Date(currentTime))
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        return RemoteViews(context.packageName, R.layout.widget_task_item).apply {
            setTextViewText(R.id.widget_task_title, task.title)
            setTextViewText(R.id.widget_task_due_date, task.dueDate.toString())
            // You can add more task details here if needed
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
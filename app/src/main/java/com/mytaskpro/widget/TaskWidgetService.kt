package com.mytaskpro.widget

import android.content.Intent
import android.util.Log
import android.widget.RemoteViewsService
import com.mytaskpro.data.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskWidgetService : RemoteViewsService() {
    companion object {
        private const val TAG = "TaskWidgetService"
    }

    @Inject
    lateinit var taskDao: TaskDao

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TaskWidgetService created")
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d(TAG, "onGetViewFactory called")
        return try {
            val appWidgetId = intent.getIntExtra(
                "appWidgetId",
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val currentTime = System.currentTimeMillis()

            Log.d(TAG, "Creating TaskWidgetRemoteViewsFactory for widget ID: $appWidgetId")
            TaskWidgetRemoteViewsFactory(applicationContext, taskDao, appWidgetId, currentTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating RemoteViewsFactory", e)
            // Return a dummy factory in case of error
            object : RemoteViewsFactory {
                override fun onCreate() {}
                override fun onDataSetChanged() {}
                override fun onDestroy() {}
                override fun getCount(): Int = 0
                override fun getViewAt(position: Int) = null
                override fun getLoadingView() = null
                override fun getViewTypeCount(): Int = 1
                override fun getItemId(position: Int): Long = position.toLong()
                override fun hasStableIds(): Boolean = true
            }
        }
    }
}
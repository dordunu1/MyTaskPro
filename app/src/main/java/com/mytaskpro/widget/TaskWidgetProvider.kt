package com.mytaskpro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.mytaskpro.MainActivity
import com.mytaskpro.R


class TaskWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "TaskWidgetProvider"
        const val ACTION_DATA_UPDATED = "com.mytaskpro.ACTION_DATA_UPDATED"
        private const val MAX_TASKS = 5 // Limit to 5 upcoming tasks
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_DATA_UPDATED || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(intent.component)
            onUpdate(context, appWidgetManager, appWidgetIds)
            Log.d(TAG, "Received ${intent.action}, updating widgets")
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d(TAG, "Updating widget $appWidgetId")
        try {
            val views = RemoteViews(context.packageName, R.layout.task_widget_layout)

            // Set up the RemoteViews object to use TaskWidgetService
            val intent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("MAX_TASKS", MAX_TASKS)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, intent)

            // Set the empty view
            views.setEmptyView(R.id.widget_list_view, R.id.empty_view)

            // Set up the intent for item clicks
            val pendingIntent = Intent(context, MainActivity::class.java).let { clickIntent ->
                PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            views.setPendingIntentTemplate(R.id.widget_list_view, pendingIntent)

            // Set up the refresh button
            val refreshIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget $appWidgetId", e)
        }
    }
}
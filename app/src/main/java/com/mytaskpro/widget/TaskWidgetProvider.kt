package com.mytaskpro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
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

    private val themeChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                forceUpdateAllWidgets(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        context.applicationContext.registerReceiver(themeChangeReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.applicationContext.unregisterReceiver(themeChangeReceiver)
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

    fun forceWidgetUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
        Log.d(TAG, "Forced widget update for ${appWidgetIds.size} widgets")
    }

    private fun forceUpdateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
        onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "Forced update for all widgets due to theme change")
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
            val isDarkMode = isDarkModeEnabled(context)
            val layoutId = if (isDarkMode) R.layout.task_widget_layout_dark else R.layout.task_widget_layout
            val views = RemoteViews(context.packageName, layoutId)

            // Set up the RemoteViews object to use TaskWidgetService
            val intent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("MAX_TASKS", MAX_TASKS)
                putExtra("IS_DARK_MODE", isDarkMode)
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
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
            Log.d(TAG, "Widget $appWidgetId updated successfully. Dark mode: $isDarkMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget $appWidgetId", e)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun isDarkModeEnabled(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = uiMode == Configuration.UI_MODE_NIGHT_YES
        Log.d(TAG, "Dark mode check: $isDark")
        return isDark
    }
}
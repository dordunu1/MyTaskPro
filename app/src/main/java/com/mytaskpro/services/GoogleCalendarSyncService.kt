package com.mytaskpro.services

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.mytaskpro.data.Task
import com.mytaskpro.data.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao
) {
    private lateinit var calendarService: Calendar

    fun setupCalendarService(accountName: String) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(CalendarScopes.CALENDAR)
        ).setSelectedAccountName(accountName)

        calendarService = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("MyTaskPro")
            .build()
    }

    suspend fun startSync(accountName: String) {
        setupCalendarService(accountName)
        syncTasksWithCalendar()
    }

    private suspend fun syncTasksWithCalendar() {
        withContext(Dispatchers.IO) {
            val tasks = taskDao.getAllTasksAsList()
            for (task in tasks) {
                val existingEvent = findExistingEvent(task)
                if (existingEvent == null) {
                    createCalendarEvent(task)
                } else {
                    updateCalendarEvent(existingEvent.id, task)
                }
            }
        }
    }

    private suspend fun findExistingEvent(task: Task): com.google.api.services.calendar.model.Event? {
        return withContext(Dispatchers.IO) {
            val events = calendarService.events().list("primary")
                .setTimeMin(DateTime(task.dueDate))
                .setTimeMax(DateTime(task.dueDate.time + 24 * 60 * 60 * 1000)) // Next 24 hours
                .setQ(task.title)
                .execute()
            events.items.firstOrNull { it.summary == task.title }
        }
    }

    private suspend fun createCalendarEvent(task: Task) {
        withContext(Dispatchers.IO) {
            val event = com.google.api.services.calendar.model.Event()
                .setSummary(task.title)
                .setDescription(task.description)

            val start = com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(DateTime(task.dueDate))
            event.start = start

            val end = com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(DateTime(task.dueDate.time + 60 * 60 * 1000)) // 1 hour duration
            event.end = end

            calendarService.events().insert("primary", event).execute()
        }
    }

    suspend fun performSync() {
        syncTasksWithCalendar()
    }

    fun stopSync() {
        // Add any cleanup logic here if needed
        // For example, cancel any ongoing sync operations
    }

    private suspend fun updateCalendarEvent(eventId: String, task: Task) {
        withContext(Dispatchers.IO) {
            val event = calendarService.events().get("primary", eventId).execute()

            event.summary = task.title
            event.description = task.description

            val start = com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(DateTime(task.dueDate))
            event.start = start

            val end = com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(DateTime(task.dueDate.time + 60 * 60 * 1000)) // 1 hour duration
            event.end = end

            calendarService.events().update("primary", eventId, event).execute()
        }
    }
}
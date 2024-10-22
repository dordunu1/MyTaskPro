package com.mytaskpro.data

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarRepository @Inject constructor(
    private val context: Context
) {
    private var calendarService: Calendar? = null

    fun setupCalendarService(accountName: String) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(CalendarScopes.CALENDAR_EVENTS)
        ).setSelectedAccountName(accountName)

        calendarService = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("MyTaskPro")
            .build()
    }

    suspend fun syncTasksWithCalendar(tasks: Flow<List<Task>>) {
        withContext(Dispatchers.IO) {
            tasks.collect { taskList ->
                taskList.forEach { task ->
                    val event = createEventFromTask(task)
                    try {
                        calendarService?.events()?.insert("primary", event)?.execute()
                    } catch (e: Exception) {
                        // Handle the exception (e.g., log it or notify the user)
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun createEventFromTask(task: Task): Event {
        return Event()
            .setSummary(task.title)
            .setDescription(task.description)
            .setStart(EventDateTime().setDateTime(DateTime(task.dueDate)))
            .setEnd(EventDateTime().setDateTime(DateTime(task.dueDate)))
    }
}
package com.mytaskpro.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mytaskpro.StorageManager
import com.mytaskpro.data.NoteDao
import com.mytaskpro.data.NoteRepository
import com.mytaskpro.data.TaskDao
import com.mytaskpro.ui.RepetitionType
import com.mytaskpro.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class RepetitiveTaskWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val taskId = inputData.getInt("taskId", -1)
            if (taskId == -1) return@withContext Result.failure()

            val task = taskDao.getTaskById(taskId) ?: return@withContext Result.failure()

            val repetitionType = RepetitionType.valueOf(inputData.getString("repetitionType") ?: return@withContext Result.failure())
            val interval = inputData.getInt("interval", 1)
            val weekDays = inputData.getString("weekDays")?.split(",")?.map { it.toInt() } ?: emptyList()
            val monthDay = inputData.getInt("monthDay", -1)
            val monthWeek = inputData.getInt("monthWeek", -1)
            val monthWeekDay = inputData.getInt("monthWeekDay", -1)
            val endDate = inputData.getLong("endDate", -1L)

            // Calculate the next occurrence
            val nextOccurrence = calculateNextOccurrence(task.dueDate, repetitionType, interval, weekDays, monthDay, monthWeek, monthWeekDay)

            if (nextOccurrence != null && (endDate == -1L || nextOccurrence.time <= endDate)) {
                // Create a new task for the next occurrence
                val newTask = task.copy(
                    id = 0,
                    dueDate = nextOccurrence,
                    reminderTime = task.reminderTime?.let { Date(nextOccurrence.time - (task.dueDate.time - it.time)) }
                )
                taskDao.insertTask(newTask)

                // Schedule the next repetition
                val taskViewModel = TaskViewModel(
                    applicationContext,
                    taskDao,
                    NoteRepository(noteDao),
                    StorageManager(applicationContext),
                    WorkManager.getInstance(applicationContext)
                )
                taskViewModel.scheduleRepetitiveTask(newTask)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun calculateNextOccurrence(
        currentDate: Date,
        repetitionType: RepetitionType,
        interval: Int,
        weekDays: List<Int>,
        monthDay: Int,
        monthWeek: Int,
        monthWeekDay: Int
    ): Date? {
        // Implement the logic to calculate the next occurrence based on the repetition type and settings
        // This is a placeholder implementation and should be replaced with actual logic
        val calendar = java.util.Calendar.getInstance()
        calendar.time = currentDate

        when (repetitionType) {
            RepetitionType.DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, interval)
            RepetitionType.WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, interval)
            RepetitionType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, interval)
            RepetitionType.YEARLY -> calendar.add(java.util.Calendar.YEAR, interval)
            else -> return null
        }

        return calendar.time
    }
}
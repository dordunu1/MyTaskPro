package com.mytaskpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>


    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Update
    suspend fun updateTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    fun getPendingTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE title = :title LIMIT 1")
    suspend fun getTaskByTitle(title: String): Task?

    // New queries to support reminder management
    @Query("SELECT * FROM tasks WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime")
    fun getUpcomingReminders(currentTime: Date): Flow<List<Task>>

    @Query("UPDATE tasks SET reminderTime = :newReminderTime WHERE id = :taskId")
    suspend fun updateReminderTime(taskId: Int, newReminderTime: Date?)

    @Query("UPDATE tasks SET reminderTime = NULL WHERE id = :taskId")
    suspend fun cancelReminder(taskId: Int)
}
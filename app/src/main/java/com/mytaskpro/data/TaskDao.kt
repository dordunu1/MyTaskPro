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
    suspend fun deleteTask(task: Task) // Changed from (task: Task?)

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    fun getPendingTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE title = :title LIMIT 1")
    suspend fun getTaskByTitle(title: String): Task?

    @Query("SELECT * FROM tasks WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime")
    fun getUpcomingReminders(currentTime: Date): Flow<List<Task>>

    @Query("UPDATE tasks SET reminderTime = :newReminderTime WHERE id = :taskId")
    suspend fun updateReminderTime(taskId: Int, newReminderTime: Date?)

    @Query("UPDATE tasks SET reminderTime = NULL WHERE id = :taskId")
    suspend fun cancelReminder(taskId: Int)

    @Query("UPDATE tasks SET isCompleted = 1, completionDate = :completionDate WHERE id = :taskId")
    suspend fun markTaskAsCompleted(taskId: Int, completionDate: Date)

    @Query("UPDATE tasks SET isCompleted = 0, completionDate = NULL WHERE id = :taskId")
    suspend fun markTaskAsIncomplete(taskId: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTaskCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY CASE WHEN completionDate IS NULL THEN 1 ELSE 0 END, completionDate DESC")
    fun getCompletedTasksSortedByDate(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND date(completionDate / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch')")
    fun getTasksCompletedOnDate(date: Date): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate >= :currentDate ORDER BY dueDate ASC LIMIT :limit")
    suspend fun getUpcomingTasks(currentDate: Long = System.currentTimeMillis(), limit: Int = 5): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC LIMIT :limit")
    suspend fun getTasksForWidget(limit: Int): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    suspend fun getPendingTaskCount(): Int

    @Query("SELECT * FROM tasks WHERE dueDate >= :startDate AND dueDate < :endDate ORDER BY dueDate ASC")
    suspend fun getTasksForDateRange(startDate: Long, endDate: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE dueDate > :currentDate AND isCompleted = 0 ORDER BY dueDate ASC LIMIT :limit")
    suspend fun getUpcomingTasks(limit: Int, currentDate: Date): List<Task>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksAsList(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Query("SELECT * FROM tasks WHERE category = :category")
    fun getTasksByCategory(category: CategoryType): Flow<List<Task>>

    @Transaction
    suspend fun replaceAllTasks(tasks: List<Task>) {
        deleteAllTasks()
        insertTasks(tasks)
    }

    @Query("SELECT * FROM tasks WHERE date(dueDate / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch') AND isCompleted = 0")
    suspend fun getTasksForDate(date: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE lastModified > :lastSyncTime")
    suspend fun getTasksModifiedSince(lastSyncTime: Long): List<Task>
}
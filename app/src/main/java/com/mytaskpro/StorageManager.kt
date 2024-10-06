package com.mytaskpro

import android.content.Context
import com.mytaskpro.data.Note
import com.mytaskpro.data.Task
import com.mytaskpro.viewmodel.UserPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(private val context: Context) {
    private val gson = Gson()

    private fun getNextId(items: List<Any>): Int {
        return (items.maxOfOrNull {
            when (it) {
                is Note -> it.id
                is Task -> it.id
                else -> 0
            }
        } ?: 0) + 1
    }

    fun saveData(notes: List<Note>, tasks: List<Task>) {
        val updatedNotes = notes.map { note ->
            note.copy(id = if (note.id == 0) getNextId(notes) else note.id)
        }
        val updatedTasks = tasks.map { task ->
            task.copy(id = if (task.id == 0) getNextId(tasks) else task.id)
        }

        val notesFile = File(context.filesDir, "notes.json")
        val tasksFile = File(context.filesDir, "tasks.json")

        notesFile.writeText(gson.toJson(updatedNotes))
        tasksFile.writeText(gson.toJson(updatedTasks))
    }

    fun loadData(): Pair<List<Note>, List<Task>> {
        val notesFile = File(context.filesDir, "notes.json")
        val tasksFile = File(context.filesDir, "tasks.json")

        val notesType = object : TypeToken<List<Note>>() {}.type
        val tasksType = object : TypeToken<List<Task>>() {}.type

        val notes = if (notesFile.exists()) {
            gson.fromJson<List<Note>>(notesFile.readText(), notesType)
        } else {
            emptyList()
        }

        val tasks = if (tasksFile.exists()) {
            gson.fromJson<List<Task>>(tasksFile.readText(), tasksType)
        } else {
            emptyList()
        }

        val uniqueNotes = notes.distinctBy { it.id }.mapIndexed { index, note ->
            note.copy(id = index + 1)
        }
        val uniqueTasks = tasks.distinctBy { it.id }.mapIndexed { index, task ->
            task.copy(id = index + 1)
        }

        return Pair(uniqueNotes, uniqueTasks)
    }

    fun deleteNote(noteId: Int) {
        val notesFile = File(context.filesDir, "notes.json")
        if (notesFile.exists()) {
            val notesType = object : TypeToken<List<Note>>() {}.type
            val notes = gson.fromJson<List<Note>>(notesFile.readText(), notesType)
            val updatedNotes = notes.filter { it.id != noteId }
            notesFile.writeText(gson.toJson(updatedNotes))
        }
    }

    fun saveUserPreferences(preferences: UserPreferences) {
        val preferencesFile = File(context.filesDir, "user_preferences.json")
        preferencesFile.writeText(gson.toJson(preferences))
    }

    fun loadUserPreferences(): UserPreferences {
        val preferencesFile = File(context.filesDir, "user_preferences.json")
        return if (preferencesFile.exists()) {
            gson.fromJson(preferencesFile.readText(), UserPreferences::class.java)
        } else {
            UserPreferences() // Return default preferences if file doesn't exist
        }
    }

    fun updateNote(updatedNote: Note) {
        val notesFile = File(context.filesDir, "notes.json")
        if (notesFile.exists()) {
            val notesType = object : TypeToken<List<Note>>() {}.type
            val notes = gson.fromJson<List<Note>>(notesFile.readText(), notesType)
            val updatedNotes = notes.map { if (it.id == updatedNote.id) updatedNote else it }
            notesFile.writeText(gson.toJson(updatedNotes))
        }
    }

    fun updateTask(updatedTask: Task) {
        val tasksFile = File(context.filesDir, "tasks.json")
        if (tasksFile.exists()) {
            val tasksType = object : TypeToken<List<Task>>() {}.type
            val tasks = gson.fromJson<List<Task>>(tasksFile.readText(), tasksType)
            val updatedTasks = tasks.map { if (it.id == updatedTask.id) updatedTask else it }
            tasksFile.writeText(gson.toJson(updatedTasks))
        }
    }

    fun updateTaskReminder(updatedTask: Task) {
        updateTask(updatedTask)
    }
}
package com.mytaskpro

import android.content.Context
import com.mytaskpro.data.Note
import com.mytaskpro.data.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(private val context: Context) {
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences("MyTaskProPreferences", Context.MODE_PRIVATE)

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

    fun savePreference(key: String, value: Any) {
        with(sharedPreferences.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("This type can't be saved into Preferences")
            }
            apply()
        }
    }

    fun getStringPreference(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun getBooleanPreference(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getIntPreference(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun getFloatPreference(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun getLongPreference(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
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
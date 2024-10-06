package com.mytaskpro.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.mytaskpro.data.Note
import com.mytaskpro.data.Task
import com.mytaskpro.data.CategoryType
import com.mytaskpro.StorageManager
import com.mytaskpro.util.ReminderWorker
import com.mytaskpro.data.NoteRepository
import com.mytaskpro.data.TaskDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mytaskpro.viewmodel.TaskAdditionStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val context: Context,
    private val taskDao: TaskDao,
    private val noteRepository: NoteRepository,
    private val storageManager: StorageManager,
    private val workManager: WorkManager
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _filterOption = MutableStateFlow<CategoryType?>(null)
    val filterOption: StateFlow<CategoryType?> = _filterOption.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DUE_DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _taskAdditionStatus = MutableStateFlow<TaskAdditionStatus>(TaskAdditionStatus.Idle)
    val taskAdditionStatus: StateFlow<TaskAdditionStatus> = _taskAdditionStatus.asStateFlow()

    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    val filteredAndSortedTasks = combine(_tasks, _filterOption, _sortOption) { tasks, filter, sort ->
        tasks.filter { task ->
            filter == null || task.category == filter
        }.sortedWith { a, b ->
            when (sort) {
                SortOption.DUE_DATE -> a.dueDate.compareTo(b.dueDate)
                SortOption.TITLE -> a.title.compareTo(b.title)
                SortOption.COMPLETED -> compareValuesBy(a, b) { it.isCompleted }
                SortOption.UNCOMPLETED -> compareValuesBy(a, b) { !it.isCompleted }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
        loadNotes()
        loadUserPreferences()
        observeTasks()
        observeNotes()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { taskList ->
                _tasks.value = taskList
            }
        }
    }

    private fun observeNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { noteList ->
                _notes.value = noteList
                saveNotes()
            }
        }
    }

    private fun loadData() {
        loadNotes()
    }

    private fun loadNotes() {
        val sharedPrefs = context.getSharedPreferences("MyTaskProPrefs", Context.MODE_PRIVATE)
        val notesJson = sharedPrefs.getString("notes", null)
        if (notesJson != null) {
            val type = object : TypeToken<List<Note>>() {}.type
            val loadedNotes = Gson().fromJson<List<Note>>(notesJson, type)
            _notes.value = loadedNotes
            Log.d("TaskViewModel", "Notes loaded: $loadedNotes")
        } else {
            Log.d("TaskViewModel", "No saved notes found")
        }
    }

    private fun saveNotes() {
        val sharedPrefs = context.getSharedPreferences("MyTaskProPrefs", Context.MODE_PRIVATE)
        val notesJson = Gson().toJson(_notes.value)
        sharedPrefs.edit().putString("notes", notesJson).apply()
        Log.d("TaskViewModel", "Notes saved: ${_notes.value}")
    }

    private fun loadUserPreferences() {
        viewModelScope.launch {
            _userPreferences.value = storageManager.loadUserPreferences()
        }
    }

    private fun saveUserPreferences() {
        viewModelScope.launch {
            storageManager.saveUserPreferences(_userPreferences.value)
        }
    }

    fun addTask(title: String, description: String, category: CategoryType, dueDate: Date, reminderTime: Date?, notifyOnDueDate: Boolean) {
        viewModelScope.launch {
            try {
                val newTask = Task(
                    title = title,
                    description = description,
                    category = category,
                    dueDate = dueDate,
                    reminderTime = reminderTime,
                    notifyOnDueDate = notifyOnDueDate
                )
                val insertedId = taskDao.insertTask(newTask)
                Log.d("TaskViewModel", "Task created with ID: $insertedId")

                val insertedTask = taskDao.getTaskById(insertedId.toInt())
                if (insertedTask != null) {
                    Log.d("TaskViewModel", "Retrieved task after insertion: $insertedTask")
                    _taskAdditionStatus.value = TaskAdditionStatus.Success
                    scheduleNotifications(insertedTask)
                } else {
                    Log.e("TaskViewModel", "Failed to retrieve inserted task")
                    _taskAdditionStatus.value = TaskAdditionStatus.Error
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding task: ${e.message}")
                _taskAdditionStatus.value = TaskAdditionStatus.Error
            }
        }
    }

    fun updateTask(taskId: Int, title: String, description: String, category: CategoryType, dueDate: Date, reminderTime: Date?, notifyOnDueDate: Boolean) {
        viewModelScope.launch {
            val updatedTask = Task(taskId, title, description, category, dueDate, reminderTime, notifyOnDueDate = notifyOnDueDate)
            taskDao.updateTask(updatedTask)
            scheduleNotifications(updatedTask)
        }
    }

    private fun scheduleNotifications(task: Task) {
        // Schedule reminder notification
        if (task.reminderTime != null) {
            scheduleNotification(task, task.reminderTime, isReminder = true)
        }

        // Schedule due date notification
        if (task.notifyOnDueDate) {
            scheduleNotification(task, task.dueDate, isReminder = false)
        }
    }

    fun updateReminder(taskId: Int, newReminderTime: Date) {
        viewModelScope.launch {
            taskDao.updateReminderTime(taskId, newReminderTime)
            val updatedTask = taskDao.getTaskById(taskId)
            updatedTask?.let { scheduleNotifications(it) }
        }
    }

    fun toggleSnoozeOptions(taskId: Int) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedTask = task.copy(showSnoozeOptions = !task.showSnoozeOptions)
            taskDao.updateTask(updatedTask)
        }
    }

    fun snoozeTask(taskId: Int, snoozeDuration: Long) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val newDueDate = Date(System.currentTimeMillis() + snoozeDuration)
            val newReminderTime = Date(System.currentTimeMillis() + snoozeDuration)

            val updatedTask = task.copy(
                dueDate = newDueDate,
                reminderTime = newReminderTime,
                isSnoozed = true,
                snoozeCount = task.snoozeCount + 1
            )
            taskDao.updateTask(updatedTask)
            scheduleNotifications(updatedTask)
            Log.d("TaskViewModel", "Task snoozed: ${task.title}, New due date: $newDueDate")
        }
    }

    fun undoSnooze(taskId: Int) {
        viewModelScope.launch {
            Log.d("TaskViewModel", "undoSnooze called for task $taskId")
            try {
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    Log.d("TaskViewModel", "Found task: $task")
                    val updatedTask = task.copy(
                        dueDate = task.dueDate,
                        reminderTime = task.reminderTime,
                        isSnoozed = false,
                        snoozeCount = 0,
                        showSnoozeOptions = false
                    )
                    taskDao.updateTask(updatedTask)
                    Log.d("TaskViewModel", "Task updated: $updatedTask")
                    scheduleNotifications(updatedTask)
                } else {
                    Log.e("TaskViewModel", "Task not found for id $taskId")
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task: ${e.message}")
            }
        }
    }

    fun cancelReminder(taskId: Int) {
        workManager.cancelUniqueWork("reminder_$taskId")
        workManager.cancelUniqueWork("due_date_$taskId")
        viewModelScope.launch {
            taskDao.cancelReminder(taskId)
        }
    }

    private fun scheduleNotification(task: Task, notificationTime: Date, isReminder: Boolean) {
        val currentTime = System.currentTimeMillis()
        val delay = notificationTime.time - currentTime

        if (delay > 0) {
            val notificationType = if (isReminder) "reminder" else "due_date"
            val workRequestBuilder = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "taskId" to task.id,
                        "taskTitle" to task.title,
                        "taskDescription" to task.description,
                        "isReminder" to isReminder,
                        "snoozeDuration" to (15 * 60 * 1000L) // Default 15 minutes snooze duration
                    )
                )

            val workRequest = workRequestBuilder.build()

            workManager.enqueueUniqueWork(
                "${notificationType}_${task.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d("TaskViewModel", "Scheduled $notificationType notification for task ${task.id} at ${notificationTime}")
        }
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedTask = task.copy(isCompleted = isCompleted)
            taskDao.updateTask(updatedTask)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                taskDao.deleteTask(task)
                workManager.cancelUniqueWork("reminder_$taskId")
            } else {
                Log.e("TaskViewModel", "Task not found for deletion: $taskId")
            }
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val outputFile = File(context.filesDir, fileName)
            inputStream?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error copying image", e)
            null
        }
    }

    private fun copyPdfToInternalStorage(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            Log.d("TaskViewModel", "Attempting to copy PDF from URI: $uri")
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(uri) ?: "pdf_${System.currentTimeMillis()}.pdf"
            val outputFile = File(context.filesDir, fileName)
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("TaskViewModel", "PDF copied successfully: ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error copying PDF: ${e.message}", e)
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }
        return fileName
    }

    fun addNote(title: String, content: String, category: CategoryType, photoPath: String?, scannedText: String?, imageUris: List<String>, pdfUris: List<String>) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "Adding note: $title")
                Log.d("TaskViewModel", "PDF URIs before processing: $pdfUris")

                val copiedImagePaths = imageUris.mapNotNull { uri ->
                    copyImageToInternalStorage(Uri.parse(uri))
                }
                val copiedPdfPaths = pdfUris.mapNotNull { uri ->
                    Log.d("TaskViewModel", "Copying PDF: $uri")
                    copyPdfToInternalStorage(uri)
                }

                Log.d("TaskViewModel", "Copied PDF Paths: $copiedPdfPaths")

                val newNote = Note(
                    id = 0, // Room will auto-generate the ID
                    title = title,
                    content = content,
                    category = category,
                    createdAt = Date(),
                    modifiedAt = Date(),
                    photoPath = photoPath,
                    scannedText = scannedText,
                    imageUris = copiedImagePaths,
                    pdfUris = copiedPdfPaths
                )
                noteRepository.insertNote(newNote)
                saveNotes()
                Log.d("TaskViewModel", "Note added: $title, Copied Image Paths: $copiedImagePaths, Copied PDF Paths: $copiedPdfPaths")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding note", e)
            }
        }
    }
    fun updateNote(note: Note) {
        viewModelScope.launch {
            try {
                val updatedImagePaths = note.imageUris.mapNotNull { uri ->
                    if (uri.startsWith("content://")) {
                        copyImageToInternalStorage(Uri.parse(uri))
                    } else {
                        uri
                    }
                }
                val updatedPdfPaths = note.pdfUris.mapNotNull { uri ->
                    if (uri.startsWith("content://")) {
                        copyPdfToInternalStorage(uri)
                    } else {
                        uri
                    }
                }
                val updatedNote = note.copy(
                    imageUris = updatedImagePaths,
                    pdfUris = updatedPdfPaths,
                    modifiedAt = Date()
                )
                noteRepository.updateNote(updatedNote)
                saveNotes()
                Log.d("TaskViewModel", "Note updated: ${note.title}, Updated Image Paths: $updatedImagePaths, Updated PDF Paths: $updatedPdfPaths")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating note", e)
            }
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            try {
                val noteToDelete = noteRepository.getNoteById(noteId)
                if (noteToDelete != null) {
                    noteRepository.deleteNote(noteToDelete)
                    saveNotes()
                    Log.d("TaskViewModel", "Note deleted successfully: $noteId")
                } else {
                    Log.e("TaskViewModel", "Note not found for deletion: $noteId")
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting note: ${e.message}")
            }
        }
    }

    fun updateFilterOption(option: CategoryType?) {
        _filterOption.value = option
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun resetTaskAdditionStatus() {
        _taskAdditionStatus.value = TaskAdditionStatus.Idle
    }

    fun updateDefaultReminderTime(minutes: Int) {
        _userPreferences.value = _userPreferences.value.copy(defaultReminderTimeMinutes = minutes)
        saveUserPreferences()
    }

    fun updateNotificationSound(sound: String) {
        _userPreferences.value = _userPreferences.value.copy(notificationSound = sound)
        saveUserPreferences()
    }
}

enum class SortOption(val displayName: String) {
    DUE_DATE("Due Date"),
    TITLE("Title"),
    COMPLETED("Completed"),
    UNCOMPLETED("Uncompleted")
}

data class UserPreferences(
    val defaultReminderTimeMinutes: Int = 15,
    val notificationSound: String = "default"
)
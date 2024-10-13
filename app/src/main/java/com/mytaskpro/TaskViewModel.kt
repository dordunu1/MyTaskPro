package com.mytaskpro.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.mytaskpro.data.*
import com.mytaskpro.StorageManager
import com.mytaskpro.util.ReminderWorker
import com.mytaskpro.util.RepetitiveTaskWorker
import com.mytaskpro.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mytaskpro.ui.RepetitionType
import java.time.ZoneId
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.mytaskpro.SettingsViewModel
import kotlinx.coroutines.TimeoutCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Context,
    private val taskDao: TaskDao,
    private val noteRepository: NoteRepository,
    private val storageManager: StorageManager,
    private val firebaseTaskRepository: FirebaseTaskRepository,
    private val workManager: WorkManager,
    private val firebaseAuth: FirebaseAuth,
    private val settingsViewModel: SettingsViewModel, // Inject SettingsViewModel
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application as Application) {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private fun getUserTasksCollection(userId: String) =
        firestore.collection("users").document(userId).collection("tasks")

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _filterOption = MutableStateFlow<FilterOption>(FilterOption.All)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DUE_DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _taskAdditionStatus = MutableStateFlow<TaskAdditionStatus>(TaskAdditionStatus.Idle)
    val taskAdditionStatus: StateFlow<TaskAdditionStatus> = _taskAdditionStatus.asStateFlow()

    private val _userPreferences = MutableStateFlow(UserPreferences())

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    enum class SyncStatus { Idle, Syncing, Success, Error }

    private val _completedTaskCount = MutableStateFlow(0)
    val completedTaskCount: StateFlow<Int> = _completedTaskCount.asStateFlow()

    private val _isUserSignedIn = MutableStateFlow(false)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn

    private val updateWidgetJob = Job()
    private val updateWidgetScope = CoroutineScope(Dispatchers.Default + updateWidgetJob)

    private val _customCategories = MutableStateFlow<List<CategoryType>>(emptyList())
    val customCategories: StateFlow<List<CategoryType>> = _customCategories.asStateFlow()

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CategoryType::class.java, CategoryTypeAdapter())
        .create()

    val filteredAndSortedTasks = combine(
        _tasks,
        _filterOption,
        _sortOption,
        _customCategories
    ) { tasks, filter, sort, customCategories ->
        tasks.filter { task ->
            when (filter) {
                is FilterOption.All -> true
                is FilterOption.Category -> {
                    val matches = task.category.type == filter.category.type &&
                            task.category.displayName == filter.category.displayName &&
                            !task.isCompleted
                    Log.d("TaskViewModel", "Category filter: task=${task.title}, category=${task.category}, filterCategory=${filter.category}, isCompleted=${task.isCompleted}, matches=$matches")
                    matches
                }
                is FilterOption.Completed -> task.isCompleted
                is FilterOption.CustomCategory -> {
                    val matches = task.category.type == "CUSTOM" &&
                            task.category.displayName == filter.category.displayName &&
                            !task.isCompleted
                    Log.d("TaskViewModel", "CustomCategory filter: task=${task.title}, category=${task.category}, filterCategory=${filter.category}, isCompleted=${task.isCompleted}, matches=$matches")
                    matches
                }
                else -> false
            }
        }.sortedWith { a, b ->
            when (sort) {
                SortOption.DUE_DATE -> a.dueDate.compareTo(b.dueDate)
                SortOption.TITLE -> a.title.compareTo(b.title)
                SortOption.COMPLETED -> compareValuesBy(b, a) { it.isCompleted }
                SortOption.UNCOMPLETED -> compareValuesBy(a, b) { it.isCompleted }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadNotes()
        loadUserPreferences()
        loadCustomCategories()
        observeTasks()
        observeNotes()
        checkUserSignInStatus()
    }

    private fun checkUserSignInStatus() {
        _isUserSignedIn.value = firebaseAuth.currentUser != null
        _currentUser.value = firebaseAuth.currentUser
        if (_isUserSignedIn.value) {
            setupFirestoreListener()
        }
    }

    fun getTaskById(id: Int): Flow<Task?> {
        return flow {
            emit(taskDao.getTaskById(id))
        }
    }

    fun createCustomCategory(categoryName: String) {
        viewModelScope.launch {
            val newCategory = CategoryType("CUSTOM", categoryName, Icons.Default.Label, CategoryType.generateRandomColor())
            _customCategories.value = _customCategories.value + newCategory
            // You might also want to save this to a local database or Firebase
            // depending on your app's architecture
        }
    }

    private fun loadNotes() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences(
            "MyTaskProPrefs",
            Context.MODE_PRIVATE
        )
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

    private fun loadUserPreferences() {
        viewModelScope.launch {
            val savedSortOption = storageManager.getStringPreference("sortOption", SortOption.DUE_DATE.name)
            _sortOption.value = try {
                SortOption.valueOf(savedSortOption)
            } catch (e: IllegalArgumentException) {
                SortOption.DUE_DATE
            }

            val savedFilterOption = storageManager.getStringPreference("filterOption", FilterOption.All.toStorageString())
            _filterOption.value = FilterOption.fromString(savedFilterOption)
        }
    }

    private fun loadCustomCategories() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences(
            "MyTaskProPrefs",
            Context.MODE_PRIVATE
        )
        val customCategoriesJson = sharedPrefs.getString("customCategories", null)
        if (customCategoriesJson != null) {
            val type = object : TypeToken<List<CategoryType>>() {}.type
            val loadedCategories =
                gson.fromJson<List<CategoryType>>(customCategoriesJson, type)
            _customCategories.value = loadedCategories
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { taskList ->
                _tasks.value = taskList
                _completedTaskCount.value = taskList.count { it.isCompleted }
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

    private fun saveNotes() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences(
            "MyTaskProPrefs",
            Context.MODE_PRIVATE
        )
        val notesJson = Gson().toJson(_notes.value)
        sharedPrefs.edit().putString("notes", notesJson).apply()
        Log.d("TaskViewModel", "Notes saved: ${_notes.value}")
    }

    private fun saveUserPreferences() {
        viewModelScope.launch {
            storageManager.savePreference("sortOption", _sortOption.value.name)
            storageManager.savePreference("filterOption", _filterOption.value.toStorageString())
        }
    }

    fun signInWithGoogle(googleSignInAccount: GoogleSignInAccount, onComplete: (AuthResult?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isUserSignedIn.value = true
                    _currentUser.value = firebaseAuth.currentUser
                    viewModelScope.launch {
                        syncTasksWithFirebase()
                        setupFirestoreListener()
                    }
                }
                onComplete(task.result)
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _isUserSignedIn.value = false
        _currentUser.value = null
    }

    fun syncTasksWithFirebase() {
        viewModelScope.launch {
            Log.d("TaskViewModel", "Starting sync process")
            settingsViewModel.startSync()
            try {
                val userId = firebaseAuth.currentUser?.uid ?: run {
                    Log.e("TaskViewModel", "User not authenticated")
                    settingsViewModel.endSync(false)
                    return@launch
                }

                withTimeout(60000) { // 60 seconds timeout
                    Log.d("TaskViewModel", "Fetching local tasks")
                    val localTasks = taskDao.getAllTasksAsList()
                    Log.d("TaskViewModel", "Local tasks count: ${localTasks.size}")

                    Log.d("TaskViewModel", "Uploading local tasks to Firebase")
                    val userTasksCollection = getUserTasksCollection(userId)
                    localTasks.forEach { task ->
                        userTasksCollection.document(task.id.toString()).set(task).await()
                    }

                    Log.d("TaskViewModel", "Fetching remote tasks")
                    val remoteTasks = userTasksCollection.get().await().toObjects(Task::class.java)
                    Log.d("TaskViewModel", "Remote tasks count: ${remoteTasks.size}")

                    Log.d("TaskViewModel", "Merging tasks")
                    val mergedTasks = mergeTasks(localTasks, remoteTasks)
                    Log.d("TaskViewModel", "Merged tasks count: ${mergedTasks.size}")

                    Log.d("TaskViewModel", "Updating local database")
                    taskDao.deleteAllTasks()
                    taskDao.insertTasks(mergedTasks)

                    _tasks.value = mergedTasks

                    Log.d("TaskViewModel", "Sync completed successfully")
                    settingsViewModel.endSync(true)
                }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutCancellationException -> Log.e("TaskViewModel", "Sync timed out", e)
                    else -> Log.e("TaskViewModel", "Sync failed", e)
                }
                settingsViewModel.endSync(false)
            }
        }
    }


    private fun mergeTasks(localTasks: List<Task>, remoteTasks: List<Task>): List<Task> {
        val mergedTasks = mutableMapOf<Int, Task>()

        // Add all local tasks
        localTasks.forEach { mergedTasks[it.id] = it }

        // Merge remote tasks, resolving conflicts
        remoteTasks.forEach { remoteTask ->
            val localTask = mergedTasks[remoteTask.id]
            if (localTask == null || remoteTask.lastModified > localTask.lastModified) {
                mergedTasks[remoteTask.id] = remoteTask
            }
        }

        return mergedTasks.values.toList()
    }

    // Override existing addTask function
    fun addTask(
        title: String,
        description: String,
        category: CategoryType,
        dueDate: Date,
        reminderTime: Date?,
        notifyOnDueDate: Boolean,
        repetitiveSettings: RepetitiveTaskSettings?
    ) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "Adding task with category: $category")
                val newTask = Task(
                    title = title,
                    description = description,
                    category = category,
                    dueDate = dueDate,
                    reminderTime = reminderTime,
                    notifyOnDueDate = notifyOnDueDate,
                    repetitiveSettings = repetitiveSettings
                )
                Log.d("TaskViewModel", "New task created: $newTask")
                val insertedId = taskDao.insertTask(newTask)
                Log.d("TaskViewModel", "Task created with ID: $insertedId")

                val insertedTask = taskDao.getTaskById(insertedId.toInt())
                if (insertedTask != null) {
                    Log.d("TaskViewModel", "Retrieved task after insertion: $insertedTask")
                    _taskAdditionStatus.value = TaskAdditionStatus.Success
                    scheduleNotifications(insertedTask)
                    if (repetitiveSettings != null) {
                        scheduleRepetitiveTask(insertedTask)
                    }
                    updateWidget()

                    // Sync with Firebase
                    if (isUserSignedIn.value) {
                        val userId = firebaseAuth.currentUser?.uid
                        if (userId != null) {
                            firestore.collection("users").document(userId)
                                .collection("tasks").document(insertedTask.id.toString())
                                .set(insertedTask)
                        }
                    }
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

    // Override existing updateTask function
    fun updateTask(
        taskId: Int,
        title: String,
        description: String,
        category: CategoryType,
        dueDate: Date,
        reminderTime: Date?,
        notifyOnDueDate: Boolean,
        repetitiveSettings: RepetitiveTaskSettings?
    ) {
        viewModelScope.launch {
            val updatedTask = Task(
                taskId,
                title,
                description,
                category,
                dueDate,
                reminderTime,
                notifyOnDueDate = notifyOnDueDate,
                repetitiveSettings = repetitiveSettings
            )
            taskDao.updateTask(updatedTask)
            scheduleNotifications(updatedTask)
            if (repetitiveSettings != null) {
                scheduleRepetitiveTask(updatedTask)
            } else {
                cancelRepetitiveTask(taskId)
            }
            updateWidget()

            // Sync with Firebase
            if (isUserSignedIn.value) {
                val userId = firebaseAuth.currentUser?.uid
                if (userId != null) {
                    firestore.collection("users").document(userId)
                        .collection("tasks").document(taskId.toString())
                        .set(updatedTask)
                }
            }
        }
    }


    // Override existing deleteTask function
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                taskDao.deleteTask(task)
                workManager.cancelUniqueWork("reminder_$taskId")
                cancelRepetitiveTask(taskId)
                updateWidget()

                // Sync with Firebase
                if (isUserSignedIn.value) {
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId != null) {
                        firestore.collection("users").document(userId)
                            .collection("tasks").document(taskId.toString())
                            .delete()
                    }
                }
            } else {
                Log.e("TaskViewModel", "Task not found for deletion: $taskId")
            }
        }
    }

    private fun setupFirestoreListener() {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            getUserTasksCollection(userId).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TaskViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (doc in snapshot.documentChanges) {
                        val task = doc.document.toObject(Task::class.java)
                        when (doc.type) {
                            DocumentChange.Type.ADDED -> handleAddedTask(task)
                            DocumentChange.Type.MODIFIED -> handleModifiedTask(task)
                            DocumentChange.Type.REMOVED -> handleRemovedTask(task.id)
                        }
                    }
                }
            }
        }
    }

    private fun handleAddedTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
            updateWidget()
        }
    }

    private fun handleModifiedTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
            updateWidget()
        }
    }

    private fun handleRemovedTask(taskId: Int) {
        viewModelScope.launch {
            taskDao.deleteTaskById(taskId)
            updateWidget()
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

    fun scheduleRepetitiveTask(task: Task) {
        val repetitiveSettings = task.repetitiveSettings ?: return
        val workRequest = OneTimeWorkRequestBuilder<RepetitiveTaskWorker>()
            .setInputData(
                workDataOf(
                    "taskId" to task.id,
                    "repetitionType" to repetitiveSettings.type.name,
                    "interval" to repetitiveSettings.interval,
                    "weekDays" to repetitiveSettings.weekDays.joinToString(","),
                    "monthDay" to (repetitiveSettings.monthDay ?: -1),
                    "monthWeek" to (repetitiveSettings.monthWeek ?: -1),
                    "monthWeekDay" to (repetitiveSettings.monthWeekDay ?: -1),
                    "endDate" to (repetitiveSettings.endDate ?: -1L)
                )
            )
            .setInitialDelay(calculateNextOccurrence(task), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "repetitive_task_${task.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelRepetitiveTask(taskId: Int) {
        workManager.cancelUniqueWork("repetitive_task_$taskId")
    }

    private fun calculateNextOccurrence(task: Task): Long {
        val repetitiveSettings = task.repetitiveSettings
            ?: return 24 * 60 * 60 * 1000L // Default to 24 hours if no settings

        val calendar = Calendar.getInstance()
        calendar.time = task.dueDate

        when (repetitiveSettings.type) {
            RepetitionType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, repetitiveSettings.interval)
            }

            RepetitionType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, repetitiveSettings.interval)
                if (repetitiveSettings.weekDays.isNotEmpty()) {
                    while (!repetitiveSettings.weekDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }

            RepetitionType.MONTHLY -> {
                calendar.add(Calendar.MONTH, repetitiveSettings.interval)
                repetitiveSettings.monthDay?.let { day ->
                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        day.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    )
                }
                if (repetitiveSettings.monthWeek != null && repetitiveSettings.monthWeekDay != null) {
                    calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, repetitiveSettings.monthWeek)
                    calendar.set(Calendar.DAY_OF_WEEK, repetitiveSettings.monthWeekDay)
                }
            }

            RepetitionType.YEARLY -> {
                calendar.add(Calendar.YEAR, repetitiveSettings.interval)
            }

            else -> return 24 * 60 * 60 * 1000L // Default to 24 hours for unsupported types
        }

        // Check if the calculated date is after the end date (if set)
        repetitiveSettings.endDate?.let { endDate ->
            val endDateMillis =
                endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (calendar.timeInMillis > endDateMillis) {
                return -1 // Indicate that no more occurrences should be scheduled
            }
        }

        return calendar.timeInMillis - System.currentTimeMillis()
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
                snoozeCount = task.snoozeCount + 1,
            )
            taskDao.updateTask(updatedTask)
            scheduleNotifications(updatedTask)
            updateWidget()

            // Sync with Firebase
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users").document(userId)
                    .collection("tasks").document(taskId.toString())
                    .set(updatedTask)
                    .addOnSuccessListener {
                        Log.d("TaskViewModel", "Task snoozed and synced with Firebase: ${task.title}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TaskViewModel", "Error syncing snoozed task with Firebase", e)
                    }
            }

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
                        showSnoozeOptions = false,
                    )
                    taskDao.updateTask(updatedTask)
                    Log.d("TaskViewModel", "Task updated: $updatedTask")
                    scheduleNotifications(updatedTask)
                    updateWidget()

                    // Sync with Firebase
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId != null) {
                        firestore.collection("users").document(userId)
                            .collection("tasks").document(taskId.toString())
                            .set(updatedTask)
                            .addOnSuccessListener {
                                Log.d("TaskViewModel", "Task unsnooze synced with Firebase: ${task.title}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("TaskViewModel", "Error syncing unsnooze task with Firebase", e)
                            }
                    }
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
            updateWidget()
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

            Log.d(
                "TaskViewModel",
                "Scheduled $notificationType notification for task ${task.id} at ${notificationTime}"
            )
        }
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedTask = task.copy(isCompleted = isCompleted)
            taskDao.updateTask(updatedTask)
            if (isCompleted) {
                cancelRepetitiveTask(taskId)
                _completedTaskCount.value += 1
            } else {
                updatedTask.repetitiveSettings?.let { scheduleRepetitiveTask(updatedTask) }
                _completedTaskCount.value -= 1
            }
            updateWidget()
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val outputFile = File(getApplication<Application>().filesDir, fileName)
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
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(uri) ?: "pdf_${System.currentTimeMillis()}.pdf"
            val outputFile = File(getApplication<Application>().filesDir, fileName)
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
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            ?.use { cursor ->
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

    fun addNote(
        title: String,
        content: String,
        category: CategoryType,
        photoPath: String?,
        scannedText: String?,
        imageUris: List<String>,
        pdfUris: List<String>
    ) {
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
                Log.d(
                    "TaskViewModel",
                    "Note added: $title, Copied Image Paths: $copiedImagePaths, Copied PDF Paths: $copiedPdfPaths"
                )
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
                Log.d(
                    "TaskViewModel",
                    "Note updated: ${note.title}, Updated Image Paths: $updatedImagePaths, Updated PDF Paths: $updatedPdfPaths"
                )
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating note", e)
            }
        }
    }

    fun updateTaskRepetitiveSettings(taskId: Int, newSettings: RepetitiveTaskSettings?) {
        viewModelScope.launch {
            try {
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    val updatedTask = task.copy(repetitiveSettings = newSettings)
                    taskDao.updateTask(updatedTask)

                    // Update the UI state immediately
                    _tasks.value = _tasks.value.map { if (it.id == taskId) updatedTask else it }

                    // Cancel existing repetitive task
                    cancelRepetitiveTask(taskId)

                    // Schedule new repetitive task if settings are not null
                    if (newSettings != null) {
                        scheduleRepetitiveTask(updatedTask)
                    }

                    updateWidget()
                    Log.d("TaskViewModel", "Updated repetitive settings for task $taskId")
                } else {
                    Log.e("TaskViewModel", "Task not found for id $taskId")
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating repetitive settings: ${e.message}")
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

    fun updateFilterOption(option: FilterOption) {
        _filterOption.value = option
        saveUserPreferences()
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
        saveUserPreferences()
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


    private fun updateWidget() {
        updateWidgetScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    val intent = Intent(getApplication(), TaskWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        val ids = AppWidgetManager.getInstance(getApplication())
                            .getAppWidgetIds(ComponentName(getApplication(), TaskWidgetProvider::class.java))
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    getApplication<Application>().sendBroadcast(intent)
                    Log.d("TaskViewModel", "Widget update broadcast sent")
                    // Force an immediate update
                    (getApplication() as? Context)?.let { context ->
                        TaskWidgetProvider().forceWidgetUpdate(context)
                    }
                } catch (e: Exception) {
                    Log.e("TaskViewModel", "Error updating widget", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateWidgetJob.cancel()
    }

    enum class SortOption(val displayName: String) {
        DUE_DATE("Due Date"),
        TITLE("Title"),
        COMPLETED("Completed"),
        UNCOMPLETED("Uncompleted")
    }

    sealed class FilterOption {
        object All : FilterOption()
        data class Category(val category: CategoryType) : FilterOption()
        object Completed : FilterOption()
        data class CustomCategory(val category: CategoryType) : FilterOption()

        companion object {
            fun fromString(value: String): FilterOption {
                return when (value.uppercase()) {
                    "ALL" -> All
                    "COMPLETED" -> Completed
                    else -> All
                }
            }
        }

        fun toStorageString(): String {
            return when (this) {
                is All -> "ALL"
                is Completed -> "COMPLETED"
                is Category -> "CATEGORY_${category.toString()}"
                is CustomCategory -> "CUSTOM_CATEGORY_${category.displayName}"
                else -> "UNKNOWN" // This covers any potential future FilterOption subclasses
            }
        }
    }

    data class UserPreferences(
        val defaultReminderTimeMinutes: Int = 15,
        val notificationSound: String = "default"
    )
}
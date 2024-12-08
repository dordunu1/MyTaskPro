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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.TimeoutCancellationException
import com.google.firebase.firestore.DocumentChange
import com.mytaskpro.domain.BadgeManager
import com.mytaskpro.repository.BadgeRepository
import com.mytaskpro.ui.TimeFrame
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import com.mytaskpro.data.NoteTypeAdapter
import com.mytaskpro.domain.TaskCompletionBadgeEvaluator
import com.mytaskpro.services.GoogleCalendarSyncService
import com.mytaskpro.data.TaskPriority
import com.mytaskpro.managers.AchievementBadgesManager


@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Context,
    private val taskDao: TaskDao,
    private val noteRepository: NoteRepository,
    private val storageManager: StorageManager,
    private val firebaseTaskRepository: FirebaseTaskRepository,
    private val workManager: WorkManager,
    private val firebaseAuth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>, // Make sure this type is correct
    private val firestore: FirebaseFirestore,
    private val badgeRepository: BadgeRepository,
    private val badgeManager: BadgeManager,
    private val googleCalendarSyncService: GoogleCalendarSyncService,
    private val badgeEvaluator: TaskCompletionBadgeEvaluator,
    private val achievementBadgesManager: AchievementBadgesManager,
    private val customCategoryDao: CustomCategoryDao

) : AndroidViewModel(application as Application) {


    fun refreshTask(taskId: Int) {
        viewModelScope.launch {
            getTaskById(taskId).collect { updatedTask ->
                _currentTask.value = updatedTask
            }
        }
    }
    data class UpcomingTask(
        val id: Int,
        val title: String,
        val dueDate: Date,
        val dueTime: Date,
        val category: CategoryType
    )

    val is24HourFormat = dataStore.data
        .map { preferences ->
            preferences[booleanPreferencesKey("is_24_hour_format")] ?: false
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    private val _upcomingTasks = MutableStateFlow<Map<LocalDate, List<UpcomingTask>>>(emptyMap())
    val upcomingTasks: StateFlow<Map<LocalDate, List<UpcomingTask>>> = _upcomingTasks.asStateFlow()

    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()


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

    private val _tasksCompleted = MutableStateFlow(0)
    val tasksCompleted: StateFlow<Int> = _tasksCompleted.asStateFlow()

    enum class SyncStatus { Idle, Syncing, Success, Error }

    private val _completedTaskCount = MutableStateFlow(0)
    val completedTaskCount: StateFlow<Int> = _completedTaskCount.asStateFlow()

    private val _isUserSignedIn = MutableStateFlow(false)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn

    private val _completionPercentage = MutableStateFlow(0f)
    val completionPercentage: StateFlow<Float> = _completionPercentage.asStateFlow()

    private val updateWidgetJob = Job()
    private val updateWidgetScope = CoroutineScope(Dispatchers.Default + updateWidgetJob)

    private val _customCategories = MutableStateFlow<List<CategoryType>>(emptyList())
    val customCategories: StateFlow<List<CategoryType>> = _customCategories.asStateFlow()

    private val _activeCategoryTypes = MutableStateFlow<Set<CategoryType>>(setOf())
    val activeCategoryTypes: StateFlow<Set<CategoryType>> = _activeCategoryTypes.asStateFlow()

    private val _currentBadge = MutableStateFlow<Badge>(Badge.NONE)
    val currentBadge: StateFlow<Badge> = _currentBadge.asStateFlow()

    private val _showBadgeAchievement = MutableStateFlow<Badge?>(null)
    val showBadgeAchievement: StateFlow<Badge?> = _showBadgeAchievement.asStateFlow()

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CategoryType::class.java, CategoryTypeAdapter())
        .registerTypeAdapter(Note::class.java, NoteTypeAdapter())
        .create()

    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    private val _showCategorySelection = MutableStateFlow(false)
    val showCategorySelection: StateFlow<Boolean> = _showCategorySelection.asStateFlow()

    private val _notificationUpdateTrigger = MutableStateFlow(0)
    val notificationUpdateTrigger: StateFlow<Int> = _notificationUpdateTrigger.asStateFlow()

    private fun triggerNotificationUpdate() {
        _notificationUpdateTrigger.value += 1
    }

    fun startGoogleCalendarSync(accountName: String) {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing
                googleCalendarSyncService.startSync(accountName)
                _syncStatus.value = SyncStatus.Success
                // You might want to show a success message to the user
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error starting Google Calendar sync", e)
                _syncStatus.value = SyncStatus.Error
                // Show an error message to the user
            }
        }
    }


    fun getTodaysTasks(): Flow<List<Task>> {
        return tasks.map { allTasks ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            allTasks.filter {
                it.dueDate.time >= today.time &&
                        it.dueDate.time < today.time + 24*60*60*1000 &&
                        !it.isCompleted
            }
        }
    }

    fun initializeAchievementBadges(userId: String) {
        viewModelScope.launch {
            achievementBadgesManager.initializeForUser(userId)
        }
    }

    fun onTaskCompleted(userId: String) {
        viewModelScope.launch {
            achievementBadgesManager.onTaskCompleted(userId)
        }
    }

    fun getNextBadgeInfo(): Pair<Badge, Int> {
        return achievementBadgesManager.getNextBadgeInfo()
    }



    fun getTaskCountForToday(): Flow<Int> {
        return tasks.map { taskList ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            taskList.count { task ->
                val taskDate = Calendar.getInstance().apply { time = task.dueDate }.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                taskDate == today && !task.isCompleted
            }
        }
    }


    fun showCategorySelectionDialog() {
        _showCategorySelection.value = true
    }

    fun hideCategorySelectionDialog() {
        _showCategorySelection.value = false
    }

    fun showConfetti() {
        _showConfetti.value = true
    }

    fun showAddTaskDialog() {
        _showAddTaskDialog.value = true
    }

    fun hideAddTaskDialog() {
        _showAddTaskDialog.value = false
    }

    fun updateCompletedTaskCount(count: Int) {
        _completedTaskCount.value = count
        evaluateBadges()
    }


    private fun updateBadge() {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            val currentBadge = _currentBadge.value
            val newBadge = badgeEvaluator.evaluate(currentBadge, _tasksCompleted.value)
            if (newBadge != currentBadge) {
                _currentBadge.value = newBadge
                _showBadgeAchievement.value = newBadge
                badgeManager.evaluateUserBadge(userId) // This will update the badge in the repository
            }
        }
    }

    fun incrementTasksCompleted() {
        _tasksCompleted.value += 1
        updateBadge()
    }

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
                is FilterOption.Priority -> task.priority == filter.priority
                else -> false
            }
        }.sortedWith { a, b ->
            when (sort) {
                SortOption.DUE_DATE -> a.dueDate.compareTo(b.dueDate)
                SortOption.TITLE -> a.title.compareTo(b.title)
                SortOption.COMPLETED -> compareValuesBy(b, a) { it.isCompleted }
                SortOption.UNCOMPLETED -> compareValuesBy(a, b) { it.isCompleted }
                SortOption.PRIORITY -> when {
                    a.priority == b.priority -> 0
                    a.priority == TaskPriority.HIGH || (a.priority == TaskPriority.MEDIUM && b.priority == TaskPriority.LOW) -> -1
                    else -> 1
                }
                else -> 0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun initializeCompletedTaskCount() {
        viewModelScope.launch {
            val completedCount = taskDao.getAllTasksAsList().count { it.isCompleted }
            _completedTaskCount.value = completedCount
            _tasksCompleted.value = completedCount
            evaluateBadges()
        }
    }

    init {
        loadNotes()
        loadUserPreferences()
        loadCustomCategories()
        observeTasks()
        observeNotes()
        checkUserSignInStatus()
        updateCompletionPercentage()
        observeCurrentBadge()
        fetchUpcomingTasks()
        initializeCompletedTaskCount()  // Add this line
        viewModelScope.launch {
            _tasks.collect { tasks ->
                // Update active categories based on non-completed tasks
                val activeCategories = tasks
                    .filter { !it.isCompleted }
                    .map { it.category }
                    .toSet()
                _activeCategoryTypes.value = activeCategories + setOf(CategoryType.COMPLETED)
            }
        }

        // Collect custom categories from database
        viewModelScope.launch {
            customCategoryDao.getAllCustomCategories().collect { categories ->
                _customCategories.value = categories.map { it.toCategoryType() }
            }
        }
    }

    private fun observeCurrentBadge() {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            badgeRepository.getBadgeInfoForUser(userId).collect { badgeInfo ->
                _currentBadge.value = badgeInfo?.currentBadge ?: Badge.NONE
            }
        }
    }

    fun evaluateBadges() {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            val oldBadge = _currentBadge.value
            val newBadge = badgeEvaluator.evaluate(oldBadge, _completedTaskCount.value)
            if (newBadge != oldBadge) {
                _currentBadge.value = newBadge
                _showBadgeAchievement.value = newBadge
                _showConfetti.value = true // Trigger confetti for new badge
                badgeManager.evaluateUserBadge(userId) // This will update the badge in the repository
            }
        }
    }

    fun dismissBadgeAchievement() {
        _showBadgeAchievement.value = null
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
            val newCategory = CustomCategory(
                type = "CUSTOM",
                displayName = categoryName,
                color = CategoryType.generateRandomColor()
            )
            customCategoryDao.insertCustomCategory(newCategory)
        }
    }

    private fun loadNotes() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences(
            "MyTaskProPrefs",
            Context.MODE_PRIVATE
        )
        val notesJson = sharedPrefs.getString("notes", null)
        if (notesJson != null) {
            try {
                val type = object : TypeToken<List<Note>>() {}.type
                val loadedNotes = gson.fromJson<List<Note>>(notesJson, type)
                _notes.value = loadedNotes
                Log.d("TaskViewModel", "Notes loaded successfully: ${loadedNotes.size} notes")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error loading notes", e)
                _notes.value = emptyList() // Fallback to empty list if there's an error
            }
        } else {
            Log.d("TaskViewModel", "No saved notes found")
            _notes.value = emptyList()
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
            _syncStatus.value = SyncStatus.Syncing
            try {
                val userId = firebaseAuth.currentUser?.uid ?: run {
                    Log.e("TaskViewModel", "User not authenticated")
                    _syncStatus.value = SyncStatus.Error
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
                    _syncStatus.value = SyncStatus.Success
                }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutCancellationException -> Log.e("TaskViewModel", "Sync timed out", e)
                    else -> Log.e("TaskViewModel", "Sync failed", e)
                }
                _syncStatus.value = SyncStatus.Error
            }
        }
    }



    fun saveTasksLocally() {
        viewModelScope.launch {
            try {
                val tasks = taskDao.getAllTasksAsList()
                storageManager.saveData(emptyList(), tasks) // Assuming you're not saving notes here
                Log.d("TaskViewModel", "Tasks saved locally: ${tasks.size}")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error saving tasks locally", e)
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
        repetitiveSettings: RepetitiveTaskSettings?,
        priority: TaskPriority // Add this parameter
    ) {
        viewModelScope.launch {
            try {
                Log.d("TaskViewModel", "Adding task with category: $category and priority: $priority")
                val newTask = Task(
                    title = title,
                    description = description,
                    category = category,
                    dueDate = dueDate,
                    reminderTime = reminderTime,
                    notifyOnDueDate = notifyOnDueDate,
                    repetitiveSettings = repetitiveSettings,
                    priority = priority // Add this field
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

                    // Update completion percentage
                    updateCompletionPercentage()
                } else {
                    Log.e("TaskViewModel", "Failed to retrieve inserted task")
                    _taskAdditionStatus.value = TaskAdditionStatus.Error
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding task: ${e.message}")
                _taskAdditionStatus.value = TaskAdditionStatus.Error
            }
            evaluateBadges()
            triggerNotificationUpdate()
            fetchUpcomingTasks()
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
        repetitiveSettings: RepetitiveTaskSettings?,
        priority: TaskPriority // Add this parameter
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
                repetitiveSettings = repetitiveSettings,
                priority = priority // Add this field
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

            // Update completion percentage
            updateCompletionPercentage()
            triggerNotificationUpdate()
            fetchUpcomingTasks()
        }
    }

    // Add a new function to update task priority
    fun updateTaskPriority(taskId: Int, newPriority: TaskPriority) {
        viewModelScope.launch {
            try {
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    val updatedTask = task.copy(priority = newPriority)
                    taskDao.updateTask(updatedTask)
                    Log.d("TaskViewModel", "Updated priority for task $taskId to $newPriority")
                } else {
                    Log.e("TaskViewModel", "Task not found for id $taskId")
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task priority: ${e.message}")
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
            updateCompletionPercentage()
            fetchUpcomingTasks()
        }
    }

    fun refreshUpcomingTasks() {
        fetchUpcomingTasks()
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

    fun fetchUpcomingTasks() {
        viewModelScope.launch {
            val currentDate = LocalDate.now()
            val sevenDaysLater = currentDate.plusDays(7)
            val tasks = taskDao.getTasksForDateRange(
                currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                sevenDaysLater.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )

            val groupedTasks = tasks
                .filter { !it.isCompleted }
                .map { task ->
                    UpcomingTask(
                        id = task.id,
                        title = task.title,
                        dueDate = task.dueDate,
                        dueTime = task.dueDate,
                        category = task.category
                    )
                }
                .groupBy { task ->
                    task.dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }

            _upcomingTasks.value = groupedTasks
        }
    }

    fun showTaskSummaryGraph() {
        // This will be implemented later to show the task summary graph
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

        // Convert LocalDate to timestamp if it exists
        val endDateTimestamp = repetitiveSettings.endDate?.let {
            it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } ?: -1L

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
                    "endDateTimestamp" to endDateTimestamp  // Changed from endDate to endDateTimestamp
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

            refreshTasks()

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

    private fun refreshTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { tasks ->
                _tasks.value = tasks
            }
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
            try {
                val task = taskDao.getTaskById(taskId) ?: return@launch
                val updatedTask = task.copy(
                    isCompleted = isCompleted,
                    completionDate = if (isCompleted) Date() else null
                )
                taskDao.updateTask(updatedTask)

                if (isCompleted) {
                    cancelRepetitiveTask(taskId)
                    _completedTaskCount.value += 1
                    _tasksCompleted.value += 1  // Update both counters
                    _showConfetti.value = true

                    // Sync with Firebase if user is signed in
                    firebaseAuth.currentUser?.uid?.let { userId ->
                        firestore.collection("users").document(userId)
                            .collection("tasks").document(taskId.toString())
                            .set(updatedTask)
                    }
                } else {
                    updatedTask.repetitiveSettings?.let { scheduleRepetitiveTask(updatedTask) }
                    _completedTaskCount.value -= 1
                    _tasksCompleted.value -= 1  // Update both counters
                }

                updateWidget()
                updateCompletionPercentage()
                triggerNotificationUpdate()
                fetchUpcomingTasks()

                // Evaluate badges immediately after updating task completion
                evaluateBadges()

                // Force refresh tasks
                refreshTasks()

                // Log the current state
                Log.d("TaskViewModel", "Completed tasks count: ${_completedTaskCount.value}")
                Log.d("TaskViewModel", "Tasks completed: ${_tasksCompleted.value}")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task completion: ${e.message}")
            }
        }
    }

    fun hideConfetti() {
        _showConfetti.value = false
    }


    private fun updateCompletionPercentage() {
        viewModelScope.launch {
            val allTasks = taskDao.getAllTasksAsList()
            val completedTasks = allTasks.count { it.isCompleted }
            val totalTasks = allTasks.size
            _completionPercentage.value = if (totalTasks > 0) {
                completedTasks.toFloat() / totalTasks.toFloat()
            } else {
                0f
            }
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


    fun getCompletionData(timeFrame: TimeFrame): Flow<List<Pair<Int, Int>>> = flow {
        when (timeFrame) {
            TimeFrame.DAILY -> {
                val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                emit(getDailyData(sevenDaysAgo, 7))
            }
            TimeFrame.WEEKLY -> {
                val fourWeeksAgo = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -3) }
                emit(getWeeklyData(fourWeeksAgo, 4))
            }
            TimeFrame.MONTHLY -> {
                val twelveMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -11) }
                emit(getMonthlyData(twelveMonthsAgo, 12))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun getDailyData(startDate: Calendar, days: Int): List<Pair<Int, Int>> {
        return (0 until days).map { dayOffset ->
            val date = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val startOfDay = date.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val endOfDay = date.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

            val tasksForDay = taskDao.getTasksForDateRange(startOfDay, endOfDay)
            val completedTasks = tasksForDay.count { it.isCompleted }
            Pair(completedTasks, tasksForDay.size)
        }
    }

    private suspend fun getWeeklyData(startDate: Calendar, weeks: Int): List<Pair<Int, Int>> {
        return (0 until weeks).map { weekOffset ->
            val weekStart = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.WEEK_OF_YEAR, weekOffset)
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val weekEnd = Calendar.getInstance().apply {
                time = weekStart.time
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val tasksForWeek = taskDao.getTasksForDateRange(weekStart.timeInMillis, weekEnd.timeInMillis)
            val completedTasks = tasksForWeek.count { it.isCompleted }
            Pair(completedTasks, tasksForWeek.size)
        }
    }

    private suspend fun getMonthlyData(startDate: Calendar, months: Int): List<Pair<Int, Int>> {
        return (0 until months).map { monthOffset ->
            val monthStart = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.MONTH, monthOffset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val monthEnd = Calendar.getInstance().apply {
                time = monthStart.time
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val tasksForMonth = taskDao.getTasksForDateRange(monthStart.timeInMillis, monthEnd.timeInMillis)
            val completedTasks = tasksForMonth.count { it.isCompleted }
            Pair(completedTasks, tasksForMonth.size)
        }
    }

    fun getCategoryCompletionData(timeFrame: TimeFrame): Flow<Map<CategoryType, List<Pair<Int, Int>>>> = flow {
        when (timeFrame) {
            TimeFrame.DAILY -> {
                val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                emit(getCategoryData(sevenDaysAgo, 7, Calendar.DAY_OF_YEAR))
            }
            TimeFrame.WEEKLY -> {
                val fourWeeksAgo = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -3) }
                emit(getCategoryData(fourWeeksAgo, 4, Calendar.WEEK_OF_YEAR))
            }
            TimeFrame.MONTHLY -> {
                val twelveMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -11) }
                emit(getCategoryData(twelveMonthsAgo, 12, Calendar.MONTH))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun getCategoryData(startDate: Calendar, periods: Int, calendarField: Int): Map<CategoryType, List<Pair<Int, Int>>> {
        val categoryData = mutableMapOf<CategoryType, MutableList<Pair<Int, Int>>>()

        (0 until periods).forEach { offset ->
            val periodStart = Calendar.getInstance().apply {
                time = startDate.time
                add(calendarField, offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val periodEnd = Calendar.getInstance().apply {
                time = periodStart.time
                add(calendarField, 1)
                add(Calendar.SECOND, -1)
            }

            val tasksForPeriod = taskDao.getTasksForDateRange(periodStart.timeInMillis, periodEnd.timeInMillis)

            CategoryType.values().forEach { category ->
                val tasksForCategory = tasksForPeriod.filter { it.category == category }
                val completedTasks = tasksForCategory.count { it.isCompleted }
                categoryData.getOrPut(category) { mutableListOf() }.add(Pair(completedTasks, tasksForCategory.size))
            }
        }

        return categoryData
    }

    suspend fun getProductivityScore(): Int {
        val allTasks = taskDao.getAllTasksAsList()
        val completedTasks = allTasks.count { it.isCompleted }
        val totalTasks = allTasks.size
        return if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0
    }

    suspend fun getCurrentStreak(): Int {
        var streak = 0
        var currentDate = Calendar.getInstance()

        while (true) {
            val startOfDay = currentDate.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val endOfDay = currentDate.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

            val tasksForDay = taskDao.getTasksForDateRange(startOfDay, endOfDay)
            val allCompleted = tasksForDay.isNotEmpty() && tasksForDay.all { it.isCompleted }

            if (allCompleted) {
                streak++
                currentDate.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    suspend fun getOverdueTasks(): Int {
        val currentDate = Date()
        return taskDao.getAllTasksAsList().count { !it.isCompleted && it.dueDate < currentDate }
    }

    suspend fun getAverageTurnaround(): Float {
        val completedTasks = taskDao.getAllTasksAsList().filter { it.isCompleted }
        val currentTime = System.currentTimeMillis()
        val totalTurnaround = completedTasks.sumOf { task ->
            val completionTime = task.completionDate?.time ?: currentTime
            completionTime - task.dueDate.time
        }
        return if (completedTasks.isNotEmpty())
            totalTurnaround.toFloat() / completedTasks.size / (24 * 60 * 60 * 1000)
        else 0f
    }

    fun getBasicAnalytics(): Flow<AnalyticsData> = flow {
        taskDao.getAllTasks().collect { allTasks ->
            val completedTasks = allTasks.filter { it.isCompleted }
            val pendingTasks = allTasks.filter { !it.isCompleted }
            val repetitiveTasks = allTasks.filter { it.repetitiveSettings != null }
            val tasksWithReminders = allTasks.filter { it.reminderTime != null }
            val overdueTasks = allTasks.filter { !it.isCompleted && it.dueDate.before(Date()) }
            val snoozedTasks = allTasks.filter { it.isSnoozed }
            val totalSnoozed = allTasks.sumOf { it.snoozeCount }
            val mostUsedCategory = allTasks.groupBy { it.category.displayName }
                .maxByOrNull { it.value.size }
                ?.key ?: "None"
            val completionRate = if (allTasks.isNotEmpty()) {
                (completedTasks.size.toFloat() / allTasks.size * 100).roundToInt()
            } else 0

            val summary = mapOf(
                "Total Tasks" to allTasks.size.toString(),
                "Completed Tasks" to completedTasks.size.toString(),
                "Pending Tasks" to pendingTasks.size.toString(),
                "Completion Rate" to "$completionRate%",
                "Repetitive Tasks" to repetitiveTasks.size.toString(),
                "Tasks with Reminders" to tasksWithReminders.size.toString(),
                "Overdue Tasks" to overdueTasks.size.toString(),
                "Total Times Tasks Snoozed" to totalSnoozed.toString(),
                "Most Used Category" to mostUsedCategory
            )

            val categoryBreakdown = getCategoryBreakdown(allTasks)
            val recentActivity = getRecentActivity(allTasks)

            val categoryTasks = allTasks.groupBy { it.category.displayName }
            val detailedTasks = mutableMapOf(
                "Total Tasks" to getTaskSummaries(allTasks),
                "Completed Tasks" to getTaskSummaries(completedTasks),
                "Pending Tasks" to getTaskSummaries(pendingTasks),
                "Repetitive Tasks" to getTaskSummaries(repetitiveTasks),
                "Tasks with Reminders" to getTaskSummaries(tasksWithReminders),
                "Overdue Tasks" to getTaskSummaries(overdueTasks),
                "Total Times Tasks Snoozed" to getTaskSummaries(allTasks.filter { it.snoozeCount > 0 }),
                "Most Used Category" to getTaskSummaries(allTasks.filter { it.category.displayName == mostUsedCategory })
            )

            // Add category-specific tasks
            categoryTasks.forEach { (category, tasks) ->
                detailedTasks["Category: $category"] = getTaskSummaries(tasks)
            }

            emit(AnalyticsData(summary, categoryBreakdown, recentActivity, detailedTasks))
        }
    }

    private fun getCategoryBreakdown(tasks: List<Task>): Map<String, Pair<Int, Color>> {
        return tasks.groupBy { it.category.displayName }
            .mapValues { (_, tasksInCategory) ->
                Pair(tasksInCategory.size, Color(tasksInCategory.first().category.color))
            }
    }

    private fun getTaskSummaries(tasks: List<Task>): List<TaskSummary> {
        return tasks.map { task ->
            TaskSummary(
                title = task.title,
                description = task.description, // Add this line
                category = task.category.displayName,
                categoryColor = Color(task.category.color),
                dueDate = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT' yyyy", Locale.US).format(task.dueDate),
                reminder = task.reminderTime?.let { SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT' yyyy", Locale.US).format(it) } ?: "None",
                status = if (task.isCompleted) "Completed" else "Pending"
            )
        }
    }

    private fun getRecentActivity(tasks: List<Task>): List<String> {
        return tasks.sortedByDescending { it.lastModified }
            .take(5)
            .map { task ->
                val action = if (task.isCompleted) "Completed" else "Modified"
                "$action: ${task.title} (${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(task.lastModified))})"
            }
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
        UNCOMPLETED("Uncompleted"),
        PRIORITY("Priority") // Add this line
    }

    sealed class FilterOption {
        object All : FilterOption()
        data class Category(val category: CategoryType) : FilterOption()
        object Completed : FilterOption()
        data class CustomCategory(val category: CategoryType) : FilterOption()
        data class Priority(val priority: TaskPriority) : FilterOption()

        companion object {
            fun fromString(value: String): FilterOption {
                return when {
                    value.uppercase() == "ALL" -> All
                    value.uppercase() == "COMPLETED" -> Completed
                    value.startsWith("CATEGORY_") -> {
                        val categoryString = value.removePrefix("CATEGORY_")
                        Category(CategoryType.values().find { it.type == categoryString } ?: CategoryType.WORK)
                    }
                    value.startsWith("CUSTOM_CATEGORY_") -> {
                        val categoryName = value.removePrefix("CUSTOM_CATEGORY_")
                        CustomCategory(CategoryType(type = "CUSTOM", categoryName, Icons.Default.Label, CategoryType.generateRandomColor()))
                    }
                    value.startsWith("PRIORITY_") -> {
                        val priorityString = value.removePrefix("PRIORITY_")
                        Priority(TaskPriority.valueOf(priorityString))
                    }
                    else -> All
                }
            }
        }

        fun toStorageString(): String {
            return when (this) {
                is All -> "ALL"
                is Completed -> "COMPLETED"
                is Category -> "CATEGORY_${category.type}"
                is CustomCategory -> "CUSTOM_CATEGORY_${category.displayName}"
                is Priority -> "PRIORITY_${priority.name}"
                else -> "UNKNOWN" // This covers any potential future FilterOption subclasses
            }
        }
    }

    data class UserPreferences(
        val defaultReminderTimeMinutes: Int = 15,
        val notificationSound: String = "default",
        val showConfettiOnCompletion: Boolean = true  // Add this line
    )
}




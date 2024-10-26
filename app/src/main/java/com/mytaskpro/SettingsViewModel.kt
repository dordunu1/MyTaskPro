package com.mytaskpro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.mytaskpro.billing.BillingManager
import com.mytaskpro.data.TaskPriority
import com.mytaskpro.managers.AchievementBadgesManager
import com.mytaskpro.managers.TaskSummaryGraphManager
import com.mytaskpro.services.GoogleCalendarSyncService
import com.mytaskpro.utils.StatusBarNotificationManager
import com.mytaskpro.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val statusBarNotificationManager: StatusBarNotificationManager,
    private val googleCalendarSyncService: GoogleCalendarSyncService,
    private val billingManager: BillingManager,
    val taskSummaryGraphManager: TaskSummaryGraphManager,
    private val achievementBadgesManager: AchievementBadgesManager
) : ViewModel() {

    // General Settings
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _isGoogleCalendarSyncEnabled = MutableStateFlow(false)
    val isGoogleCalendarSyncEnabled: StateFlow<Boolean> = _isGoogleCalendarSyncEnabled.asStateFlow()

    private val _isTaskPriorityEnabled = MutableStateFlow(false)
    val isTaskPriorityEnabled: StateFlow<Boolean> = _isTaskPriorityEnabled.asStateFlow()

    private val _defaultTaskPriority = MutableStateFlow(TaskPriority.MEDIUM)
    val defaultTaskPriority: StateFlow<TaskPriority> = _defaultTaskPriority.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _currentTheme = MutableStateFlow("Default")
    val currentTheme = _currentTheme.asStateFlow()

    private val _isStatusBarQuickAddEnabled = MutableStateFlow(false)
    val isStatusBarQuickAddEnabled: StateFlow<Boolean> = _isStatusBarQuickAddEnabled.asStateFlow()

    // Notification Settings
    private val _taskReminders = MutableStateFlow(true)
    val taskReminders = _taskReminders.asStateFlow()

    // Sync Settings
    private val _isGoogleSyncEnabled = MutableStateFlow(false)
    val isGoogleSyncEnabled = _isGoogleSyncEnabled.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    // Premium Features
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    private val _isTaskSummaryGraphEnabled = MutableStateFlow(false)
    val isTaskSummaryGraphEnabled: StateFlow<Boolean> = _isTaskSummaryGraphEnabled.asStateFlow()

    private val _isAchievementBadgesEnabled = MutableStateFlow(false)
    val isAchievementBadgesEnabled: StateFlow<Boolean> = _isAchievementBadgesEnabled.asStateFlow()

    private val _premiumProductDetails = MutableStateFlow<ProductDetails?>(null)
    val premiumProductDetails: StateFlow<ProductDetails?> = _premiumProductDetails.asStateFlow()

    // Widget Customization
    private val _widgetTheme = MutableStateFlow("Default")
    val widgetTheme = _widgetTheme.asStateFlow()

    private val _widgetTaskCount = MutableStateFlow(5)
    val widgetTaskCount = _widgetTaskCount.asStateFlow()

    // App Info
    private val _appVersion = MutableStateFlow("1.0.0")
    val appVersion = _appVersion.asStateFlow()

    // Force Refresh
    private val _forceRefresh = MutableStateFlow(0)
    val forceRefresh: StateFlow<Int> = _forceRefresh.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _isDarkMode.value = preferences[PreferencesKeys.DARK_MODE] ?: false
                _is24HourFormat.value = preferences[PreferencesKeys.HOUR_FORMAT] ?: false
                _isStatusBarQuickAddEnabled.value = preferences[PreferencesKeys.STATUS_BAR_QUICK_ADD] ?: false
                _isTaskSummaryGraphEnabled.value = preferences[PreferencesKeys.TASK_SUMMARY_GRAPH_ENABLED] ?: false
                _isAchievementBadgesEnabled.value = preferences[PreferencesKeys.ACHIEVEMENT_BADGES_ENABLED] ?: false
                _isTaskPriorityEnabled.value = preferences[PreferencesKeys.IS_TASK_PRIORITY_ENABLED] ?: false
                _defaultTaskPriority.value = TaskPriority.valueOf(
                    preferences[PreferencesKeys.DEFAULT_TASK_PRIORITY] ?: TaskPriority.MEDIUM.name
                )
                _isPremium.value = preferences[PreferencesKeys.IS_PREMIUM] ?: false
                updateStatusBarNotification()
            }
        }

        viewModelScope.launch {
            billingManager.purchaseFlow.collect { purchaseState ->
                when (purchaseState) {
                    is BillingManager.PurchaseState.Purchased -> {
                        _isPremium.value = true
                        dataStore.edit { preferences ->
                            preferences[PreferencesKeys.IS_PREMIUM] = true
                        }
                    }
                    else -> {} // Handle other states if needed
                }
            }
        }

        viewModelScope.launch {
            billingManager.productDetailsFlow.collect { productDetails ->
                _premiumProductDetails.value = productDetails.firstOrNull()
            }
        }
    }

    fun toggleTaskPriority() {
        viewModelScope.launch {
            val newValue = !_isTaskPriorityEnabled.value
            _isTaskPriorityEnabled.value = newValue
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_TASK_PRIORITY_ENABLED] = newValue
            }
        }
    }

    fun setDefaultTaskPriority(priority: TaskPriority) {
        viewModelScope.launch {
            _defaultTaskPriority.value = priority
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEFAULT_TASK_PRIORITY] = priority.name
            }
        }
    }

    fun toggleTaskSummaryGraph() {
        viewModelScope.launch {
            val newValue = !_isTaskSummaryGraphEnabled.value
            _isTaskSummaryGraphEnabled.value = newValue
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.TASK_SUMMARY_GRAPH_ENABLED] = newValue
            }
            taskSummaryGraphManager.setEnabled(newValue)
        }
    }

    fun toggleAchievementBadges() {
        viewModelScope.launch {
            val newValue = !_isAchievementBadgesEnabled.value
            _isAchievementBadgesEnabled.value = newValue
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ACHIEVEMENT_BADGES_ENABLED] = newValue
            }
            achievementBadgesManager.setEnabled(newValue)
        }
    }

    fun toggleGoogleCalendarSync(accountName: String) {
        viewModelScope.launch {
            val newValue = !_isGoogleCalendarSyncEnabled.value
            _isGoogleCalendarSyncEnabled.value = newValue
            if (newValue) {
                googleCalendarSyncService.startSync(accountName)
            } else {
                googleCalendarSyncService.stopSync()
            }
        }
    }

    fun onGoogleCalendarSyncEnabled(accountName: String) {
        viewModelScope.launch {
            googleCalendarSyncService.startSync(accountName)
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDarkMode.value = !_isDarkMode.value
            updateTheme()
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DARK_MODE] = _isDarkMode.value
            }
        }
    }

    fun toggle24HourFormat() {
        viewModelScope.launch(Dispatchers.Main) {
            _is24HourFormat.value = !_is24HourFormat.value
            updateTimeFormat()
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HOUR_FORMAT] = _is24HourFormat.value
            }
        }
    }

    fun toggleStatusBarQuickAdd() {
        viewModelScope.launch(Dispatchers.Main) {
            _isStatusBarQuickAddEnabled.value = !_isStatusBarQuickAddEnabled.value
            updateStatusBarNotification()
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _currentTheme.value = theme
            updateTheme()
        }
    }

    fun toggleTaskReminders() {
        viewModelScope.launch(Dispatchers.Main) {
            _taskReminders.value = !_taskReminders.value
        }
    }

    fun toggleGoogleSync() {
        viewModelScope.launch(Dispatchers.Main) {
            _isGoogleSyncEnabled.value = !_isGoogleSyncEnabled.value
        }
    }

    fun upgradeToPremium(activity: Activity) {
        viewModelScope.launch {
            val productDetails = _premiumProductDetails.value
            if (productDetails != null) {
                billingManager.launchBillingFlow(activity, productDetails)
            } else {
                Log.e("SettingsViewModel", "Product details not available")
                // Handle error (e.g., show a toast or update UI)
            }
        }
    }

    fun setWidgetTheme(theme: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _widgetTheme.value = theme
        }
    }

    fun setWidgetTaskCount(count: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            _widgetTaskCount.value = count
        }
    }

    fun setUserEmail(email: String?) {
        viewModelScope.launch(Dispatchers.Main) {
            _userEmail.value = email
        }
    }

    fun updateSignedInEmail(email: String?) {
        setUserEmail(email)
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            _isDarkMode.value = isDark
        }
    }

    fun startSync() {
        viewModelScope.launch(Dispatchers.Main) {
            Log.d("SettingsViewModel", "Starting sync")
            _syncStatus.value = SyncStatus.Syncing
            _isSyncing.value = true
        }
    }

    fun endSync(success: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            Log.d("SettingsViewModel", "Ending sync, success: $success")
            if (success) {
                updateLastSyncTime()
                _syncStatus.value = SyncStatus.Success
            } else {
                _syncStatus.value = SyncStatus.Error
            }
            _isSyncing.value = false
        }
    }

    fun resetSyncStatus() {
        viewModelScope.launch(Dispatchers.Main) {
            Log.d("SettingsViewModel", "Resetting sync status")
            _syncStatus.value = SyncStatus.Idle
            _isSyncing.value = false
        }
    }

    private fun updateLastSyncTime() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        _lastSyncTime.value = LocalDateTime.now().format(formatter)
    }

    fun triggerForceRefresh() {
        viewModelScope.launch(Dispatchers.Main) {
            _forceRefresh.value += 1
        }
    }

    fun getPlayStoreIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.mytaskpro")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getPlayStoreWebIntent(): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.mytaskpro")
        )
    }

    private fun updateStatusBarNotification() {
        viewModelScope.launch {
            try {
                statusBarNotificationManager.updateQuickAddNotification(_isStatusBarQuickAddEnabled.value)
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STATUS_BAR_QUICK_ADD] = _isStatusBarQuickAddEnabled.value
                }
                Log.d(
                    "SettingsViewModel",
                    "Status bar quick add ${if (_isStatusBarQuickAddEnabled.value) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating status bar notification", e)
                // Handle the error (e.g., show a toast or update UI)
            }
        }
    }

    private fun updateTheme() {
        // This function will be implemented in ThemeUtils
    }

    private fun updateTimeFormat() {
        TimeUtils.setUse24HourFormat(_is24HourFormat.value)
        // Trigger a UI update if necessary
        triggerForceRefresh()
    }

    private fun updateLocale() {
        // This function will be implemented to update app locale
    }

    enum class SyncStatus { Idle, Syncing, Success, Error }

    companion object {
        val GOOGLE_CALENDAR_SYNC_KEY = booleanPreferencesKey("google_calendar_sync")
    }

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HOUR_FORMAT = booleanPreferencesKey("hour_format")
        val STATUS_BAR_QUICK_ADD = booleanPreferencesKey("status_bar_quick_add")
        val TASK_SUMMARY_GRAPH_ENABLED = booleanPreferencesKey("task_summary_graph_enabled")
        val ACHIEVEMENT_BADGES_ENABLED = booleanPreferencesKey("achievement_badges_enabled")
        val IS_TASK_PRIORITY_ENABLED = booleanPreferencesKey("is_task_priority_enabled")
        val DEFAULT_TASK_PRIORITY = stringPreferencesKey("default_task_priority")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
    }
}
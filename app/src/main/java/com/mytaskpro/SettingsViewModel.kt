package com.mytaskpro

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mytaskpro.utils.StatusBarNotificationManager
import com.mytaskpro.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mytaskpro.services.GoogleCalendarSyncService



@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val statusBarNotificationManager: StatusBarNotificationManager,
    private val googleCalendarSyncService: GoogleCalendarSyncService
) : ViewModel() {

    // General Settings
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _isGoogleCalendarSyncEnabled = MutableStateFlow(false)
    val isGoogleCalendarSyncEnabled: StateFlow<Boolean> = _isGoogleCalendarSyncEnabled.asStateFlow()


    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _currentTheme = MutableStateFlow("Default")
    val currentTheme = _currentTheme.asStateFlow()

    private val _isStatusBarQuickAddEnabled = MutableStateFlow(false)
    val isStatusBarQuickAddEnabled: StateFlow<Boolean> = _isStatusBarQuickAddEnabled.asStateFlow()

    private val _availableThemes = MutableStateFlow(listOf("Default", "Light", "Dark", "Blue", "Green"))
    val availableThemes = _availableThemes.asStateFlow()

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
                updateStatusBarNotification()
            }
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

    companion object {
        val GOOGLE_CALENDAR_SYNC_KEY = booleanPreferencesKey("google_calendar_sync")
    }

    // Functions to update settings
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

    fun upgradeToPremium() {
        viewModelScope.launch(Dispatchers.Main) {
            _isPremium.value = true
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

    enum class SyncStatus { Idle, Syncing, Success, Error }

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
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.mytaskpro"))
    }

    private fun updateStatusBarNotification() {
        viewModelScope.launch {
            try {
                statusBarNotificationManager.updateQuickAddNotification(_isStatusBarQuickAddEnabled.value)
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.STATUS_BAR_QUICK_ADD] = _isStatusBarQuickAddEnabled.value
                }
                Log.d("SettingsViewModel", "Status bar quick add ${if (_isStatusBarQuickAddEnabled.value) "enabled" else "disabled"}")
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

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HOUR_FORMAT = booleanPreferencesKey("hour_format")
        val STATUS_BAR_QUICK_ADD = booleanPreferencesKey("status_bar_quick_add")
    }
}
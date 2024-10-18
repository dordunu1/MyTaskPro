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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import android.content.Intent
import android.net.Uri
import com.mytaskpro.utils.TimeUtils


class SettingsViewModel @Inject constructor() : ViewModel() {

    // General Settings
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _currentTheme = MutableStateFlow("Default")
    val currentTheme = _currentTheme.asStateFlow()

    private val _availableThemes = MutableStateFlow(listOf("Default", "Light", "Dark", "Blue", "Green"))
    val availableThemes = _availableThemes.asStateFlow()

    private val _currentLanguage = MutableStateFlow(Locale.getDefault().language)
    val currentLanguage = _currentLanguage.asStateFlow()

    private val _availableLanguages = MutableStateFlow(listOf("en", "es", "fr", "de", "it"))
    val availableLanguages = _availableLanguages.asStateFlow()

    // Notification Settings
    private val _taskReminders = MutableStateFlow(true)
    val taskReminders = _taskReminders.asStateFlow()

    private val _dailySummary = MutableStateFlow(false)
    val dailySummary = _dailySummary.asStateFlow()

    private val _dailySummaryTime = MutableStateFlow(LocalTime.of(20, 0))
    val dailySummaryTime = _dailySummaryTime.asStateFlow()

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

    // Functions to update settings
    fun toggleDarkMode() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDarkMode.value = !_isDarkMode.value
            updateTheme()
        }
    }

    fun toggle24HourFormat() {
        viewModelScope.launch(Dispatchers.Main) {
            _is24HourFormat.value = !_is24HourFormat.value
            updateTimeFormat()
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _currentTheme.value = theme
            updateTheme()
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _currentLanguage.value = language
            updateLocale()
        }
    }

    fun toggleTaskReminders() {
        viewModelScope.launch(Dispatchers.Main) {
            _taskReminders.value = !_taskReminders.value
        }
    }

    fun toggleDailySummary() {
        viewModelScope.launch(Dispatchers.Main) {
            _dailySummary.value = !_dailySummary.value
        }
    }

    fun setDailySummaryTime(time: LocalTime) {
        viewModelScope.launch(Dispatchers.Main) {
            _dailySummaryTime.value = time
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
}
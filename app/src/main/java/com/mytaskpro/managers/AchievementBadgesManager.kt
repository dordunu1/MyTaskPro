package com.mytaskpro.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mytaskpro.data.Badge
import com.mytaskpro.domain.BadgeManager
import com.mytaskpro.repository.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementBadgesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val badgeManager: BadgeManager,
    private val badgeRepository: BadgeRepository
) {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentBadge = MutableStateFlow<Badge>(Badge.NONE)
    val currentBadge: StateFlow<Badge> = _currentBadge.asStateFlow()

    private val _tasksCompleted = MutableStateFlow(0)
    val tasksCompleted: StateFlow<Int> = _tasksCompleted.asStateFlow()

    suspend fun initializeForUser(userId: String) {
        updateBadgeInfo(userId)
    }

    suspend fun onTaskCompleted(userId: String) {
        badgeManager.incrementTasksCompleted(userId)
        updateBadgeInfo(userId)
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACHIEVEMENT_BADGES_ENABLED] = enabled
        }
        _isEnabled.value = enabled
    }

    private suspend fun updateBadgeInfo(userId: String) {
        badgeRepository.getBadgeInfoForUser(userId).first()?.let { badgeInfo ->
            _currentBadge.value = badgeInfo.currentBadge
            _tasksCompleted.value = badgeInfo.tasksCompleted
        }
    }

    fun getNextBadgeInfo(): Pair<Badge, Int> {
        return when (_currentBadge.value) {
            Badge.NONE -> Pair(Badge.BRONZE, 30 - _tasksCompleted.value)
            Badge.BRONZE -> Pair(Badge.SILVER, 80 - _tasksCompleted.value)
            Badge.SILVER -> Pair(Badge.GOLD, 200 - _tasksCompleted.value)
            Badge.GOLD -> Pair(Badge.DIAMOND, 350 - _tasksCompleted.value)
            Badge.DIAMOND -> Pair(Badge.DIAMOND, 0)
        }
    }

    private object PreferencesKeys {
        val ACHIEVEMENT_BADGES_ENABLED = booleanPreferencesKey("achievement_badges_enabled")
    }
}
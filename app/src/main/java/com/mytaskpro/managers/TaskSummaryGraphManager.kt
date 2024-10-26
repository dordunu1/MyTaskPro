package com.mytaskpro.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskSummaryGraphManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.collect { preferences ->
                _isEnabled.value = preferences[PreferencesKeys.TASK_SUMMARY_GRAPH_ENABLED] ?: false
            }
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_SUMMARY_GRAPH_ENABLED] = enabled
        }
        _isEnabled.value = enabled
    }

    private object PreferencesKeys {
        val TASK_SUMMARY_GRAPH_ENABLED = booleanPreferencesKey("task_summary_graph_enabled")
    }
}
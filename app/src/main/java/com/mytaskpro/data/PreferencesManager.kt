package com.mytaskpro.data // Adjust this package name if necessary

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mytaskpro.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val dataStore: DataStore<Preferences>) {
    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
    }

    val themeFlow: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            try {
                AppTheme.valueOf(preferences[THEME_KEY] ?: AppTheme.Default.name)
            } catch (e: IllegalArgumentException) {
                AppTheme.Default
            }
        }

    suspend fun saveThemePreference(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}
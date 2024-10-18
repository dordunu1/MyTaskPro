package com.mytaskpro.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.mytaskpro.SettingsViewModel

object ThemeUtils {
    @Composable
    fun isDarkTheme(settingsViewModel: SettingsViewModel): Boolean {
        val isDarkMode = settingsViewModel.isDarkMode.value
        return when (isDarkMode) {
            true -> true
            false -> false
            else -> isSystemInDarkTheme()
        }
    }

    fun updateTheme(settingsViewModel: SettingsViewModel) {
        // This function will be called when the theme is changed in settings
        // For now, it's empty as the theme change is handled by Compose
        // You can add any additional theme-related logic here if needed
    }
}
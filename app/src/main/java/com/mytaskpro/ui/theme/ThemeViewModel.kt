package com.mytaskpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytaskpro.data.PreferencesManager
import com.mytaskpro.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _currentTheme = MutableStateFlow<AppTheme?>(null)
    val currentTheme: StateFlow<AppTheme?> = _currentTheme.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.themeFlow.collect { theme ->
                _currentTheme.value = theme
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesManager.saveThemePreference(theme)
        }
    }
}
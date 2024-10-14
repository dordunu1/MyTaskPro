package com.mytaskpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytaskpro.ai.AIRecommendationEngine
import com.mytaskpro.ai.RecommendationModel
import com.mytaskpro.data.TaskDao
import com.mytaskpro.data.UserAction
import com.mytaskpro.data.repository.AIRecommendationRepository
import com.mytaskpro.data.repository.UserActionRepository // New import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIRecommendationViewModel @Inject constructor(
    private val engine: AIRecommendationEngine,
    private val taskDao: TaskDao,
    private val recommendationRepository: AIRecommendationRepository,
    private val userActionRepository: UserActionRepository // New dependency
) : ViewModel() {
    private val _recommendations = MutableStateFlow<List<RecommendationModel>>(emptyList())
    val recommendations: StateFlow<List<RecommendationModel>> = _recommendations.asStateFlow()

    init {
        viewModelScope.launch {
            generateRecommendations()
        }
    }

    private suspend fun generateRecommendations() {
        taskDao.getAllTasks().collect { tasks ->
            val recentActions = userActionRepository.getRecentActions() // Fetch recent actions
            val newRecommendations = engine.generateRecommendations(tasks, recentActions)
            recommendationRepository.saveRecommendations(newRecommendations)
            _recommendations.value = newRecommendations
        }
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _recommendations.value = recommendationRepository.getRecommendations()
        }
    }
}
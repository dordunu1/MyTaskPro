package com.mytaskpro.data.repository

import com.mytaskpro.ai.RecommendationModel

interface AIRecommendationRepository {
    suspend fun getRecommendations(): List<RecommendationModel>
    suspend fun saveRecommendations(recommendations: List<RecommendationModel>)
}
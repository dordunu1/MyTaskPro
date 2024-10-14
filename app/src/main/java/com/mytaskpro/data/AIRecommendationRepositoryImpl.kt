package com.mytaskpro.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mytaskpro.ai.RecommendationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AIRecommendationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val context: Context
) : AIRecommendationRepository {
    private val recommendationsCollection = firestore.collection("recommendations")
    private val sharedPreferences = context.getSharedPreferences("AIRecommendations", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun getRecommendations(): List<RecommendationModel> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get recommendations from Firebase
                val snapshot = recommendationsCollection.get().await()
                val firebaseRecommendations = snapshot.documents.mapNotNull { it.toObject<RecommendationModel>() }

                if (firebaseRecommendations.isNotEmpty()) {
                    // If Firebase has recommendations, cache them locally and return
                    cacheRecommendations(firebaseRecommendations)
                    firebaseRecommendations
                } else {
                    // If Firebase is empty, return cached recommendations
                    getCachedRecommendations()
                }
            } catch (e: Exception) {
                // If there's an error (e.g., no internet), return cached recommendations
                getCachedRecommendations()
            }
        }
    }

    override suspend fun saveRecommendations(recommendations: List<RecommendationModel>) {
        withContext(Dispatchers.IO) {
            try {
                // Save to Firebase
                val batch = firestore.batch()
                recommendationsCollection.get().await().documents.forEach { batch.delete(it.reference) }
                recommendations.forEach { recommendation ->
                    val docRef = recommendationsCollection.document()
                    batch.set(docRef, recommendation)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // If Firebase save fails, log the error
            }

            // Always cache locally
            cacheRecommendations(recommendations)
        }
    }

    private fun cacheRecommendations(recommendations: List<RecommendationModel>) {
        val json = gson.toJson(recommendations)
        sharedPreferences.edit().putString("cached_recommendations", json).apply()
    }

    private fun getCachedRecommendations(): List<RecommendationModel> {
        val json = sharedPreferences.getString("cached_recommendations", null)
        return if (json != null) {
            gson.fromJson(json, object : TypeToken<List<RecommendationModel>>() {}.type)
        } else {
            emptyList()
        }
    }
}
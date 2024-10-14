package com.mytaskpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mytaskpro.data.UserAction
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class UserActionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userActionsCollection
        get() = firestore.collection("users")
            .document(auth.currentUser?.uid ?: "")
            .collection("userActions")

    suspend fun getRecentActions(limit: Int = 50): List<UserAction> {
        return try {
            userActionsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(UserAction::class.java)
        } catch (e: Exception) {
            // Log the error or handle it as needed
            emptyList()
        }
    }

    suspend fun addUserAction(action: UserAction) {
        try {
            userActionsCollection.add(action).await()
        } catch (e: Exception) {
            // Log the error or handle it as needed
        }
    }

    suspend fun clearOldActions(olderThan: Date) {
        try {
            val oldActions = userActionsCollection
                .whereLessThan("timestamp", olderThan)
                .get()
                .await()

            val batch = firestore.batch()
            oldActions.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Log the error or handle it as needed
        }
    }
}
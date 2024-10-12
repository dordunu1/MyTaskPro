package com.mytaskpro.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository
) {
    private val tasksCollection = firestore.collection("tasks")

    suspend fun syncTasks(localTasks: List<Task>): List<Task> {
        val currentUser = authRepository.currentUser.value
        val userId = currentUser?.uid ?: return localTasks
        val remoteTasks = getRemoteTasks(userId)
        val mergedTasks = mergeTasks(localTasks, remoteTasks)
        updateRemoteTasks(userId, mergedTasks)
        return mergedTasks
    }

    private suspend fun getRemoteTasks(userId: String): List<Task> {
        return tasksCollection.document(userId).collection("userTasks")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Task>() }
    }

    private suspend fun updateRemoteTasks(userId: String, tasks: List<Task>) {
        val batch = firestore.batch()
        val userTasksRef = tasksCollection.document(userId).collection("userTasks")

        // Delete all existing tasks
        userTasksRef.get().await().documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        // Add all merged tasks
        tasks.forEach { task ->
            val docRef = userTasksRef.document(task.id.toString())
            batch.set(docRef, task)
        }

        batch.commit().await()
    }

    private fun mergeTasks(localTasks: List<Task>, remoteTasks: List<Task>): List<Task> {
        val mergedTasks = mutableMapOf<Int, Task>()

        (localTasks + remoteTasks).forEach { task ->
            val existingTask = mergedTasks[task.id]
            if (existingTask == null || task.lastModified > existingTask.lastModified) {
                mergedTasks[task.id] = task
            }
        }

        return mergedTasks.values.toList()
    }
}
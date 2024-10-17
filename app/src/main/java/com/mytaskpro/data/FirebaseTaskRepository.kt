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

        // Fix: merge with priority to remote task states, especially `isCompleted`
        val mergedTasks = mergeTasksWithPriority(localTasks, remoteTasks)
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

        // Delete all existing tasks in Firestore
        userTasksRef.get().await().documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        // Add all merged tasks with priority adjustments
        tasks.forEach { task ->
            val docRef = userTasksRef.document(task.id.toString())
            batch.set(docRef, task)
        }

        batch.commit().await()
    }

    // Custom merge function with priority for `isCompleted`
    private fun mergeTasksWithPriority(localTasks: List<Task>, remoteTasks: List<Task>): List<Task> {
        val mergedTasks = mutableMapOf<Int, Task>()

        // Combine tasks from both local and remote
        (localTasks + remoteTasks).forEach { task ->
            val existingTask = mergedTasks[task.id]
            if (existingTask == null) {
                mergedTasks[task.id] = task
            } else {
                // If task exists both locally and remotely, give priority to the remote's `isCompleted` state
                val mergedTask = if (task.lastModified > existingTask.lastModified) {
                    task.copy(isCompleted = existingTask.isCompleted || task.isCompleted)
                } else {
                    existingTask.copy(isCompleted = existingTask.isCompleted || task.isCompleted)
                }
                mergedTasks[task.id] = mergedTask
            }
        }

        return mergedTasks.values.toList()
    }
}

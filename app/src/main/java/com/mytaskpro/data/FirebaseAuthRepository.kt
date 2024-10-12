package com.mytaskpro.data

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithGoogle(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            authResult.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign-in successful but user is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun refreshUser(): Result<FirebaseUser> {
        val user = auth.currentUser
        return if (user != null) {
            try {
                user.reload().await()
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No user signed in"))
        }
    }

    suspend fun getUserIdToken(forceRefresh: Boolean = false): Result<String> {
        val user = auth.currentUser
        return if (user != null) {
            try {
                val idToken = user.getIdToken(forceRefresh).await().token
                idToken?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Failed to get ID token"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No user signed in"))
        }
    }
}
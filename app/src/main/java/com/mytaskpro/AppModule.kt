package com.mytaskpro.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.mytaskpro.data.AppDatabase
import com.mytaskpro.data.NoteDao
import com.mytaskpro.data.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.mytaskpro.data.repository.AIRecommendationRepository
import com.mytaskpro.data.repository.AIRecommendationRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import com.mytaskpro.data.BadgeDao
import com.mytaskpro.data.repository.UserActionRepository
import com.mytaskpro.domain.BadgeEvaluator
import com.mytaskpro.domain.BadgeManager
import com.mytaskpro.domain.TaskCompletionBadgeEvaluator
import com.mytaskpro.repository.BadgeRepository
import com.mytaskpro.utils.StatusBarNotificationManager
import androidx.datastore.preferences.preferencesDataStore
import com.mytaskpro.data.PreferencesManager
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }

    @Provides
    @Singleton
    fun providePreferencesManager(dataStore: DataStore<Preferences>): PreferencesManager {
        return PreferencesManager(dataStore)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "task_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideStatusBarNotificationManager(
        @ApplicationContext context: Context,
        taskDao: TaskDao
    ): StatusBarNotificationManager {
        return StatusBarNotificationManager(context, taskDao)
    }

    @Provides
    @Singleton
    fun provideBadgeDao(database: AppDatabase): BadgeDao {
        return database.badgeDao()
    }

    @Provides
    @Singleton
    fun provideTaskCompletionBadgeEvaluator(): TaskCompletionBadgeEvaluator {
        return TaskCompletionBadgeEvaluator()
    }

    @Provides
    @Singleton
    fun provideBadgeManager(
        badgeRepository: BadgeRepository,
        taskCompletionBadgeEvaluator: TaskCompletionBadgeEvaluator
    ): BadgeManager {
        return BadgeManager(badgeRepository, taskCompletionBadgeEvaluator)
    }

    @Provides
    @Singleton
    fun provideUserActionRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UserActionRepository {
        return UserActionRepository(firestore, auth)
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideAIRecommendationRepository(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): AIRecommendationRepository {
        return AIRecommendationRepositoryImpl(firestore, context)
    }
}
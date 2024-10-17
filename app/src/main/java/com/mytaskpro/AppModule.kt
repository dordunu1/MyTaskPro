package com.mytaskpro.di

import android.content.Context
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "task_database"
        ).build()
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
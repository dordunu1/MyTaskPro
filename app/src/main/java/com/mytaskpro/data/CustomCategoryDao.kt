package com.mytaskpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories")
    fun getAllCustomCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCustomCategory(category: CustomCategory)

    @Query("SELECT * FROM custom_categories WHERE displayName = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CustomCategory?
} 
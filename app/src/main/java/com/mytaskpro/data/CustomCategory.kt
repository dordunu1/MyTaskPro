package com.mytaskpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val displayName: String,
    val color: Int
) {
    fun toCategoryType(): CategoryType {
        return CategoryType(type, displayName, Icons.Default.Label, color)
    }

    companion object {
        fun fromCategoryType(categoryType: CategoryType): CustomCategory {
            return CustomCategory(
                type = categoryType.type,
                displayName = categoryType.displayName,
                color = categoryType.color
            )
        }
    }
} 
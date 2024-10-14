package com.mytaskpro.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Date

@Entity(tableName = "tasks")
@TypeConverters(RepetitiveTaskSettingsConverter::class, CategoryTypeConverter::class, DateConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: CategoryType = CategoryType.WORK,
    val dueDate: Date = Date(),
    val reminderTime: Date? = null,
    val isCompleted: Boolean = false,
    val notifyOnDueDate: Boolean = true,
    val repetitiveSettings: RepetitiveTaskSettings? = null,
    val showSnoozeOptions: Boolean = false,
    val snoozeCount: Int = 0,
    val isSnoozed: Boolean = false,
    val completionDate: Date? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val creationDate: Date = Date() // Add this line
) {
    val categoryColor: Int
        get() = category.color
}

class RepetitiveTaskSettingsConverter {
    @TypeConverter
    fun fromRepetitiveTaskSettings(settings: RepetitiveTaskSettings?): String? {
        return settings?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toRepetitiveTaskSettings(settingsString: String?): RepetitiveTaskSettings? {
        return settingsString?.let { Gson().fromJson(it, RepetitiveTaskSettings::class.java) }
    }
}

class CategoryTypeConverter {
    @TypeConverter
    fun fromCategoryType(category: CategoryType): String {
        return "${category.type}:${category.displayName}:${category.color}"
    }

    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        val parts = value.split(":")
        return CategoryType(parts[0], parts[1], color = parts[2].toInt())
    }
}

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
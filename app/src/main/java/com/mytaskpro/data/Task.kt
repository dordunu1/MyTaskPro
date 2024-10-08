package com.mytaskpro.data

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.util.Date

@Entity(tableName = "tasks")
@TypeConverters(RepetitiveTaskSettingsConverter::class, CategoryTypeConverter::class, DateConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val category: CategoryType,
    val dueDate: Date,
    val reminderTime: Date? = null,
    val isCompleted: Boolean = false,
    val notifyOnDueDate: Boolean = true,
    val repetitiveSettings: RepetitiveTaskSettings? = null,
    val showSnoozeOptions: Boolean = false,
    val snoozeCount: Int = 0,
    val isSnoozed: Boolean = false,
    val completionDate: Date? = null
) {
    val categoryColor: Color
        get() = when (category) {
            is CategoryType.Custom -> category.color
            else -> Color.Unspecified
        }
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
    private val gson = GsonBuilder()
        .registerTypeAdapter(CategoryType::class.java, CategoryTypeAdapter())
        .create()

    @TypeConverter
    fun fromCategoryType(category: CategoryType): String {
        return when (category) {
            is CategoryType.Custom -> "CUSTOM:${category.displayName}:${category.color.toArgb()}"
            else -> gson.toJson(category)
        }
    }


    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        return if (value.startsWith("CUSTOM:")) {
            val parts = value.split(":")
            val name = parts[1]
            val color = Color(parts[2].toInt())
            CategoryType.Custom(name, color)
        } else {
            gson.fromJson(value, CategoryType::class.java)
        }
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
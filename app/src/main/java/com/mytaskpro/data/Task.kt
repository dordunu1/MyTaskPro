package com.mytaskpro.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.firebase.firestore.PropertyName
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

@Entity(tableName = "tasks")
@TypeConverters(RepetitiveTaskSettingsConverter::class, CategoryTypeConverter::class, DateConverter::class, LocalDateTimeListConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: CategoryType = CategoryType.WORK,
    val dueDate: Date = Date(),
    val reminderTime: Date? = null,

    @get:PropertyName("completed") @set:PropertyName("completed")
    var isCompleted: Boolean = false,

    val notifyOnDueDate: Boolean = true,
    val repetitiveSettings: RepetitiveTaskSettings? = null,
    val showSnoozeOptions: Boolean = false,
    val snoozeCount: Int = 0,

    @get:PropertyName("snoozed") @set:PropertyName("snoozed")
    var isSnoozed: Boolean = false,

    val completionDate: Date? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val creationDate: Date = Date(),
    val snoozeHistory: List<LocalDateTime> = emptyList()
) {
    @get:PropertyName("categoryColor")
    val categoryColor: Int
        get() = category.color

    fun addSnooze(dateTime: LocalDateTime): Task {
        val updatedSnoozeHistory = snoozeHistory.toMutableList()
        updatedSnoozeHistory.add(dateTime)
        return copy(
            snoozeCount = snoozeCount + 1,
            snoozeHistory = updatedSnoozeHistory,
            isSnoozed = true
        )
    }

    fun isOverdue(): Boolean {
        val now = Date()
        return !isCompleted && dueDate.before(now)
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

class LocalDateTimeListConverter {
    @TypeConverter
    fun fromLocalDateTimeList(value: List<LocalDateTime>): String {
        return value.joinToString(",") { it.toEpochSecond(ZoneOffset.UTC).toString() }
    }

    @TypeConverter
    fun toLocalDateTimeList(value: String): List<LocalDateTime> {
        return if (value.isBlank()) emptyList()
        else value.split(",").map { LocalDateTime.ofEpochSecond(it.toLong(), 0, ZoneOffset.UTC) }
    }
}
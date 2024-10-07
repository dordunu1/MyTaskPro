package com.mytaskpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import java.util.Date

@Entity(tableName = "tasks")
@TypeConverters(RepetitiveTaskSettingsConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: CategoryType,
    val dueDate: Date,
    val reminderTime: Date?,
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false,
    val snoozeCount: Int = 0,
    val showSnoozeOptions: Boolean = false,
    val notifyOnDueDate: Boolean = true,
    val repetitiveSettings: RepetitiveTaskSettings? = null
)


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
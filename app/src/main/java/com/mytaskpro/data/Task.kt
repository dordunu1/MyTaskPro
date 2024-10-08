package com.mytaskpro.data

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.util.Date

@Entity(tableName = "tasks")
@TypeConverters(RepetitiveTaskSettingsConverter::class, CategoryTypeConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: CategoryType,
    val dueDate: Date,
    val reminderTime: Date?,
    val isCompleted: Boolean = false,
    val completionDate: Date? = null,
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

class CategoryTypeConverter {
    private val gson = GsonBuilder()
        .registerTypeAdapter(CategoryType::class.java, CategoryTypeAdapter())
        .create()

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String {
        val json = gson.toJson(value)
        Log.d("CategoryTypeConverter", "Converting CategoryType to String: $value -> $json")
        return json
    }

    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        Log.d("CategoryTypeConverter", "Converting String to CategoryType: $value")
        return try {
            val jsonObject = JSONObject(value)
            if (jsonObject.has("customDisplayName")) {
                CategoryType.Custom(jsonObject.getString("customDisplayName"))
            } else {
                gson.fromJson(value, CategoryType::class.java)
            }
        } catch (e: Exception) {
            Log.e("CategoryTypeConverter", "Error converting to CategoryType: ${e.message}")
            CategoryType.UNKNOWN
        }.also {
            Log.d("CategoryTypeConverter", "Converted to CategoryType: $it")
        }
    }
}
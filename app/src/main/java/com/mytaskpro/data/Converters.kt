package com.mytaskpro.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Date
import com.mytaskpro.data.Badge

class Converters {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CategoryType::class.java, CategoryTypeAdapter())
        .create()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromBadge(badge: Badge): String = badge.name

    @TypeConverter
    fun toBadge(value: String): Badge = Badge.valueOf(value)

    @TypeConverter
    fun fromRepetitiveTaskSettings(settings: RepetitiveTaskSettings?): String? {
        return settings?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRepetitiveTaskSettings(settingsString: String?): RepetitiveTaskSettings? {
        return settingsString?.let { gson.fromJson(it, RepetitiveTaskSettings::class.java) }
    }

    @TypeConverter
    fun fromColor(color: Int): String = color.toString()

    @TypeConverter
    fun toColor(value: String): Int = value.toInt()

    @TypeConverter
    fun fromCategoryType(category: CategoryType): String {
        return "${category.type}:${category.displayName}:${category.color}"
    }

    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        val parts = value.split(":")
        return CategoryType(
            type = parts[0],
            displayName = parts[1],
            color = parts[2].toInt()
        )
    }
}
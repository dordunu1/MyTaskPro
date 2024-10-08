package com.mytaskpro.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Date

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
    fun fromRepetitiveTaskSettings(settings: RepetitiveTaskSettings?): String? {
        return settings?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRepetitiveTaskSettings(settingsString: String?): RepetitiveTaskSettings? {
        return settingsString?.let { gson.fromJson(it, RepetitiveTaskSettings::class.java) }
    }

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        return gson.fromJson(value, CategoryType::class.java)
    }
}
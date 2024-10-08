package com.mytaskpro.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.*
import java.lang.reflect.Type

class CategoryTypeAdapter : JsonSerializer<CategoryType>, JsonDeserializer<CategoryType> {
    override fun serialize(src: CategoryType, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src.type)
        jsonObject.addProperty("displayName", src.displayName)
        jsonObject.addProperty("color", src.color.toArgb())
        if (src is CategoryType.Custom) {
            jsonObject.addProperty("customDisplayName", src.displayName)
            jsonObject.addProperty("customColor", src.color.toArgb())
        }
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): CategoryType {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString
        val displayName = jsonObject.get("displayName")?.asString
        val color = jsonObject.get("color")?.asInt?.let { Color(it) } ?: CategoryType.generateRandomColor()

        return when (type) {
            "WORK" -> CategoryType.WORK
            "SCHOOL" -> CategoryType.SCHOOL
            "SOCIAL" -> CategoryType.SOCIAL
            "CRYPTO" -> CategoryType.CRYPTO
            "HEALTH" -> CategoryType.HEALTH
            "MINDFULNESS" -> CategoryType.MINDFULNESS
            "INVOICES" -> CategoryType.INVOICES
            "COMPLETED" -> CategoryType.COMPLETED
            "CUSTOM" -> {
                val customColor = jsonObject.get("customColor")?.asInt?.let { Color(it) } ?: color
                CategoryType.Custom(
                    jsonObject.get("customDisplayName")?.asString ?: displayName ?: "Custom",
                    customColor
                )
            }
            else -> CategoryType.Custom(displayName ?: "Custom", color)
        }
    }
}
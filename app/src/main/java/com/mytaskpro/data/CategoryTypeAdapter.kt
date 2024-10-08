package com.mytaskpro.data

import com.google.gson.*
import java.lang.reflect.Type

class CategoryTypeAdapter : JsonSerializer<CategoryType>, JsonDeserializer<CategoryType> {
    override fun serialize(src: CategoryType, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src.javaClass.simpleName)
        jsonObject.addProperty("displayName", src.displayName)
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): CategoryType {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString
        val displayName = jsonObject.get("displayName")?.asString

        return when (type) {
            "WORK" -> CategoryType.WORK
            "SCHOOL" -> CategoryType.SCHOOL
            "SOCIAL" -> CategoryType.SOCIAL
            "CRYPTO" -> CategoryType.CRYPTO
            "HEALTH" -> CategoryType.HEALTH
            "MINDFULNESS" -> CategoryType.MINDFULNESS
            "INVOICES" -> CategoryType.INVOICES
            "COMPLETED" -> CategoryType.COMPLETED
            "UNKNOWN" -> CategoryType.UNKNOWN
            "Custom" -> CategoryType.Custom(displayName ?: "Unknown")
            else -> CategoryType.UNKNOWN
        }
    }
}
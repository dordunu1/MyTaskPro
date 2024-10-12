package com.mytaskpro.data

import com.google.gson.*
import java.lang.reflect.Type

class CategoryTypeAdapter : JsonSerializer<CategoryType>, JsonDeserializer<CategoryType> {
    override fun serialize(src: CategoryType, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src.type)
        jsonObject.addProperty("displayName", src.displayName)
        jsonObject.addProperty("color", src.color)
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): CategoryType {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString ?: "CUSTOM"
        val displayName = jsonObject.get("displayName")?.asString ?: "Custom"
        val color = jsonObject.get("color")?.asInt ?: CategoryType.generateRandomColor()

        return CategoryType(type, displayName, color = color)
    }
}
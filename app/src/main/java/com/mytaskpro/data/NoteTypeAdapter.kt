package com.mytaskpro.data

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.*

class NoteTypeAdapter : TypeAdapter<Note>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    override fun write(out: JsonWriter, value: Note) {
        out.beginObject()
        out.name("id").value(value.id)
        out.name("title").value(value.title)
        out.name("content").value(value.content)
        out.name("category").value(value.category.toString())
        out.name("createdAt").value(dateFormat.format(value.createdAt))
        out.name("modifiedAt").value(dateFormat.format(value.modifiedAt))
        out.name("photoPath").value(value.photoPath)
        out.name("scannedText").value(value.scannedText)
        out.name("imageUris").beginArray()
        value.imageUris.forEach { out.value(it) }
        out.endArray()
        out.name("pdfUris").beginArray()
        value.pdfUris.forEach { out.value(it) }
        out.endArray()
        out.endObject()
    }

    override fun read(input: JsonReader): Note {
        var id = 0
        var title = ""
        var content = ""
        var category = CategoryType.WORK
        var createdAt = Date()
        var modifiedAt = Date()
        var photoPath: String? = null
        var scannedText: String? = null
        var imageUris = listOf<String>()
        var pdfUris = listOf<String>()

        input.beginObject()
        while (input.hasNext()) {
            when (input.nextName()) {
                "id" -> id = input.nextInt()
                "title" -> title = input.nextString()
                "content" -> content = input.nextString()
                "category" -> category = CategoryType.fromString(input.nextString())
                "createdAt" -> createdAt = dateFormat.parse(input.nextString()) ?: Date()
                "modifiedAt" -> modifiedAt = dateFormat.parse(input.nextString()) ?: Date()
                "photoPath" -> photoPath = input.nextStringOrNull()
                "scannedText" -> scannedText = input.nextStringOrNull()
                "imageUris" -> imageUris = readStringArray(input)
                "pdfUris" -> pdfUris = readStringArray(input)
                else -> input.skipValue()
            }
        }
        input.endObject()

        return Note(id, title, content, category, createdAt, modifiedAt, photoPath, scannedText, imageUris, pdfUris)
    }

    private fun readStringArray(reader: JsonReader): List<String> {
        val list = mutableListOf<String>()
        reader.beginArray()
        while (reader.hasNext()) {
            list.add(reader.nextString())
        }
        reader.endArray()
        return list
    }

    private fun JsonReader.nextStringOrNull(): String? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }
    }
}
package com.mytaskpro.data


import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: CategoryType,
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date(),
    val photoPath: String? = null,
    val scannedText: String? = null,
    val imageUris: List<String> = emptyList(),
    val pdfUris: List<String> = emptyList() // Changed from pdfUri to pdfUris
)
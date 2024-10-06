package com.mytaskpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE photoPath IS NOT NULL")
    fun getNotesWithPhotos(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE scannedText IS NOT NULL")
    fun getNotesWithScannedText(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE imageUris != '[]'")
    fun getNotesWithImages(): Flow<List<Note>>

    /// Updated query to get notes with PDFs
    @Query("SELECT * FROM notes WHERE pdfUris != '[]'")
    fun getNotesWithPdfs(): Flow<List<Note>>
}
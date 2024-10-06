package com.mytaskpro.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    fun getNotesWithPhotos(): Flow<List<Note>> = noteDao.getNotesWithPhotos()

    fun getNotesWithScannedText(): Flow<List<Note>> = noteDao.getNotesWithScannedText()

    fun getNotesWithImages(): Flow<List<Note>> = noteDao.getNotesWithImages()

    // Updated method to get notes with PDFs
    fun getNotesWithPdfs(): Flow<List<Note>> = noteDao.getNotesWithPdfs()

    suspend fun insertNoteWithDetails(
        title: String,
        content: String,
        category: CategoryType,
        photoPath: String?,
        scannedText: String?,
        imageUris: List<String>,
        pdfUris: List<String> // Changed to List<String>
    ) {
        val note = Note(
            title = title,
            content = content,
            category = category,
            photoPath = photoPath,
            scannedText = scannedText,
            imageUris = imageUris,
            pdfUris = pdfUris // Changed to pdfUris
        )
        noteDao.insertNote(note)
    }

    suspend fun updateNoteWithDetails(
        id: Int,
        title: String,
        content: String,
        category: CategoryType,
        photoPath: String?,
        scannedText: String?,
        imageUris: List<String>,
        pdfUris: List<String> // Changed to List<String>
    ) {
        val note = Note(
            id = id,
            title = title,
            content = content,
            category = category,
            photoPath = photoPath,
            scannedText = scannedText,
            imageUris = imageUris,
            pdfUris = pdfUris // Changed to pdfUris
        )
        noteDao.updateNote(note)
    }

    // New method to add a PDF to an existing note
    suspend fun addPdfToNote(noteId: Int, pdfUri: String) {
        val note = noteDao.getNoteById(noteId)
        note?.let {
            val updatedPdfUris = it.pdfUris + pdfUri
            val updatedNote = it.copy(pdfUris = updatedPdfUris)
            noteDao.updateNote(updatedNote)
        }
    }

    // New method to remove a PDF from an existing note
    suspend fun removePdfFromNote(noteId: Int, pdfUri: String) {
        val note = noteDao.getNoteById(noteId)
        note?.let {
            val updatedPdfUris = it.pdfUris - pdfUri
            val updatedNote = it.copy(pdfUris = updatedPdfUris)
            noteDao.updateNote(updatedNote)
        }
    }
}
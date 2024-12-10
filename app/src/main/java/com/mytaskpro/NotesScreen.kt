package com.mytaskpro.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytaskpro.data.Note
import com.mytaskpro.data.CategoryType
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun NotesScreen(viewModel: TaskViewModel) {
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    val notes by viewModel.notes.collectAsState(initial = emptyList())
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryType?>(null) }

    if (editingNote != null || showAddNoteDialog) {
        AddNoteDialog(
            category = editingNote?.category ?: selectedCategory ?: CategoryType("CUSTOM", "Custom", Icons.Default.Label, CategoryType.generateRandomColor()),
            existingNote = editingNote,
            onDismiss = {
                editingNote = null
                showAddNoteDialog = false
                selectedCategory = null
            },
            onNoteSaved = { title, content, category, photoPath, scannedText, imageUris, pdfUris ->
                if (editingNote != null) {
                    viewModel.updateNote(
                        editingNote!!.copy(
                            title = title,
                            content = content,
                            category = category,
                            photoPath = photoPath,
                            scannedText = scannedText,
                            imageUris = imageUris,
                            pdfUris = pdfUris,
                            modifiedAt = Date()
                        )
                    )
                } else {
                    viewModel.addNote(title, content, category, photoPath, scannedText, imageUris, pdfUris)
                }
            },
            viewModel = viewModel
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(VibrantBlue.copy(alpha = 0.05f), VibrantPurple.copy(alpha = 0.05f))
                    )
                )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📝 Notes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    CategoryFilterDropdown(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        viewModel = viewModel
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = notes.filter { selectedCategory == null || it.category == selectedCategory },
                        key = { note -> note.id }
                    ) { note ->
                        NoteItem(
                            note = note,
                            onNoteClicked = { editingNote = it },
                            onDeleteNote = { noteToDelete = it }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    tint = Color.White
                )
            }
        }
    }

    noteToDelete?.let { note ->
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteNote(note.id)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete this note?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPink)
            ) {
                Text("Delete", color = White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Gray)
            ) {
                Text("Cancel", color = White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    selectedCategory: CategoryType?,
    onCategorySelected: (CategoryType?) -> Unit,
    viewModel: TaskViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    val customCategories by viewModel.customCategories.collectAsState(initial = emptyList())
    val smallerTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.width(160.dp)
    ) {
        TextField(
            readOnly = true,
            value = selectedCategory?.displayName ?: "All Categories",
            onValueChange = { },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = selectedCategory?.let {
                {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = Color(it.color), shape = CircleShape)
                    )
                }
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            textStyle = smallerTextStyle
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // All Categories option
            DropdownMenuItem(
                text = { Text("All Categories", style = smallerTextStyle) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = VibrantBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            // Add New Category option
            DropdownMenuItem(
                text = { Text("+ New Category", style = smallerTextStyle) },
                onClick = {
                    expanded = false
                    showAddCategoryDialog = true
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = VibrantBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            // Divider between actions and categories
            Divider()

            // Custom categories
            customCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName, style = smallerTextStyle) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = Color(category.color), shape = CircleShape)
                        )
                    }
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onCategoryAdded = { categoryName ->
                viewModel.addCustomCategory(categoryName)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onCategoryAdded: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { 
                        categoryName = it
                        showError = false
                    },
                    label = { Text("Category Name") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Category name cannot be empty") }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isBlank()) {
                        showError = true
                    } else {
                        onCategoryAdded(categoryName)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NoteItem(note: Note, onNoteClicked: (Note) -> Unit, onDeleteNote: (Note) -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onNoteClicked(note) }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color = Color(note.category.color), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        note.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // Display images
                if (note.imageUris.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        note.imageUris.forEach { uriString ->
                            val uri = if (uriString.startsWith("content://")) {
                                Uri.parse(uriString)
                            } else {
                                Uri.fromFile(File(uriString))
                            }
                            AsyncImage(
                                model = uri,
                                contentDescription = "Note image",
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(100.dp)
                                    .padding(end = 8.dp)
                                    .clickable { /* Handle image click */ },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Display PDFs
                if (note.pdfUris.isNotEmpty()) {
                    Text(
                        "Attached PDFs: ${note.pdfUris.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    note.pdfUris.take(2).forEach { pdfUri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(pdfUri), "application/pdf")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                                        } else {
                                            Toast.makeText(context, "No PDF viewer app installed", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error opening PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        Log.e("NoteItem", "Error opening PDF", e)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = VibrantPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Uri.parse(pdfUri).lastPathSegment ?: "Unknown PDF",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = VibrantBlue
                            )
                        }
                    }
                    if (note.pdfUris.size > 2) {
                        Text(
                            "+${note.pdfUris.size - 2} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    "Created: ${dateFormat.format(note.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray
                )
                Text(
                    "Modified: ${dateFormat.format(note.modifiedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onDeleteNote(note) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = VibrantPink
                    )
                }
            }
        }
    }
}
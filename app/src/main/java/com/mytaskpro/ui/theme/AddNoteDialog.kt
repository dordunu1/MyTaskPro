package com.mytaskpro.ui.theme

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.mytaskpro.ScannerActivity
import com.mytaskpro.data.CategoryType
import com.mytaskpro.data.Note
import com.mytaskpro.util.ImageUtils
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddNoteDialog(
    category: CategoryType,
    existingNote: Note?,
    onDismiss: () -> Unit,
    onNoteSaved: (String, String, CategoryType, String?, String?, List<String>, List<String>) -> Unit
) {
    var noteContent by remember { mutableStateOf(TextFieldValue(existingNote?.let { it.title + "\n" + it.content } ?: "")) }
    var selectedCategory by remember { mutableStateOf(category) }
    val photoPath by remember { mutableStateOf(existingNote?.photoPath) }
    var scannedText by remember { mutableStateOf(existingNote?.scannedText ?: "") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedImageUris by remember { mutableStateOf<List<String>>(existingNote?.imageUris ?: emptyList()) }
    var newlySelectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedPdfUris by remember { mutableStateOf<List<String>>(existingNote?.pdfUris ?: emptyList()) }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val newScannedText = result.data?.getStringExtra("SCANNED_TEXT")
            newScannedText?.let {
                scannedText += if (scannedText.isNotEmpty()) "\n$it" else it
                noteContent = TextFieldValue(
                    text = noteContent.text + (if (noteContent.text.isNotEmpty()) "\n" else "") + it,
                    selection = TextRange(noteContent.text.length + it.length + 1)
                )
                focusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(existingNote) {
        Log.d("AddNoteDialog", "Existing note image URIs: ${existingNote?.imageUris}")
        Log.d("AddNoteDialog", "Existing note PDF URIs: ${existingNote?.pdfUris}")
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column {
            TopActionBar(
                onBack = onDismiss,
                onSave = {
                    val (title, content) = splitTitleAndContent(noteContent.text)
                    val allImageUris = selectedImageUris + newlySelectedImageUris.map { it.toString() }
                    onNoteSaved(title, content, selectedCategory, photoPath, scannedText, allImageUris, selectedPdfUris)
                    isEditing = false  // Switch back to view mode
                },
                onEdit = { isEditing = !isEditing },
                isEditing = isEditing,
                category = selectedCategory,
                onCategoryChanged = { selectedCategory = it }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    CustomTextField(
                        value = noteContent,
                        onValueChange = { newValue -> if (isEditing) noteContent = newValue },
                        textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .focusRequester(focusRequester),
                        readOnly = !isEditing
                    )
                }

                items(selectedImageUris + newlySelectedImageUris.map { it.toString() }) { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp)
                            .clickable { expandedImageUri = uri }
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isEditing) {
                            IconButton(
                                onClick = {
                                    selectedImageUris = selectedImageUris - uri
                                    newlySelectedImageUris = newlySelectedImageUris.filter { it.toString() != uri }
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                items(selectedPdfUris) { uri ->
                    PdfPreview(
                        uri = uri,
                        onRemove = { if (isEditing) selectedPdfUris = selectedPdfUris - uri }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(200.dp))
                }
            }

            if (isEditing) {
                BottomActionBar(
                    onImagePicked = { uri ->
                        uri?.let {
                            newlySelectedImageUris = newlySelectedImageUris + it
                            Log.d("AddNoteDialog", "Image added: $it")
                            noteContent = TextFieldValue(
                                text = noteContent.text + "\n",
                                selection = TextRange(noteContent.text.length + 1)
                            )
                            focusRequester.requestFocus()
                        }
                    },
                    onPdfPicked = { uri ->
                        uri?.let {
                            selectedPdfUris = selectedPdfUris + it.toString()
                            Log.d("AddNoteDialog", "PDF added: $it")
                        }
                    },
                    onScanRequested = {
                        val intent = Intent(context, ScannerActivity::class.java)
                        scanLauncher.launch(intent)
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    expandedImageUri?.let { uri ->
        Dialog(
            onDismissRequest = { expandedImageUri = null },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Expanded image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun splitTitleAndContent(text: String): Pair<String, String> {
    val lines = text.lines()
    return if (lines.isNotEmpty()) {
        Pair(lines.first(), lines.drop(1).joinToString("\n"))
    } else {
        Pair("", "")
    }
}

@Composable
fun TopActionBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    isEditing: Boolean,
    category: CategoryType,
    onCategoryChanged: (CategoryType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                CategoryButton(category = category, onCategoryChanged = onCategoryChanged)
            }
            IconButton(onClick = { /* TODO: Implement share action */ }) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(
                onClick = if (isEditing) onSave else onEdit
            ) {
                Icon(
                    if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save" else "Edit",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun BottomActionBar(
    onImagePicked: (Uri?) -> Unit,
    onPdfPicked: (Uri?) -> Unit,
    onScanRequested: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = Icons.Default.DocumentScanner,
            text = "Scan",
            onClick = onScanRequested
        )
        ImagePicker(onImagePicked = onImagePicked, onPdfPicked = onPdfPicked)
    }
}

@Composable
fun CustomTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle,
    cursorBrush: Brush,
    placeholderColor: Color,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly
        )
        if (value.text.isEmpty()) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Header\n")
                    }
                    append("Start typing...")
                },
                style = textStyle.copy(color = placeholderColor)
            )
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun CategoryButton(category: CategoryType, onCategoryChanged: (CategoryType) -> Unit) {
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { showCategoryDropdown = true }
    ) {
        Icon(category.icon, contentDescription = "Category", tint = MaterialTheme.colorScheme.onPrimary)
        Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
    }

    DropdownMenu(
        expanded = showCategoryDropdown,
        onDismissRequest = { showCategoryDropdown = false }
    ) {
        CategoryType.values().forEach { categoryType ->
            DropdownMenuItem(
                text = { Text(categoryType.displayName) },
                onClick = {
                    onCategoryChanged(categoryType)
                    showCategoryDropdown = false
                },
                leadingIcon = {
                    Icon(categoryType.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
    }
}

@Composable
fun ImagePicker(onImagePicked: (Uri?) -> Unit, onPdfPicked: (Uri?) -> Unit) {
    var showImagePickerOptions by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { showImagePickerOptions = true }
    ) {
        Icon(Icons.Default.Photo, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onPrimary)
        Text("Gallery", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
    }

    if (showImagePickerOptions) {
        AlertDialog(
            onDismissRequest = { showImagePickerOptions = false },
            title = { Text("Choose File") },
            text = {
                Column {
                    val context = LocalContext.current
                    val imageUri = remember { mutableStateOf<Uri?>(null) }

                    val galleryLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        imageUri.value = uri
                        Log.d("ImagePicker", "Gallery image selected: $uri")
                        onImagePicked(uri)
                        showImagePickerOptions = false
                    }

                    val cameraLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicture()
                    ) { success: Boolean ->
                        if (success) {
                            Log.d("ImagePicker", "Camera image captured: ${imageUri.value}")
                            onImagePicked(imageUri.value)
                            showImagePickerOptions = false
                        }
                    }

                    val pdfLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        Log.d("PdfPicker", "PDF selected: $uri")
                        onPdfPicked(uri)
                        showImagePickerOptions = false
                    }

                    Button(onClick = {
                        Log.d("ImagePicker", "Launching gallery picker")
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Choose from Gallery")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val uri = ImageUtils.createImageUri(context)
                        imageUri.value = uri
                        Log.d("ImagePicker", "Launching camera with uri: $uri")
                        cameraLauncher.launch(uri)
                    }) {
                        Text("Take Photo")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        Log.d("PdfPicker", "Launching PDF picker")
                        pdfLauncher.launch("application/pdf")
                    }) {
                        Text("Choose PDF")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImagePickerOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PdfPreview(uri: String, onRemove: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                try {
                    val pdfFile = File(Uri.parse(uri).path ?: "")
                    val pdfUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(pdfUri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(Intent.createChooser(intent, "Open PDF"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open PDF", Toast.LENGTH_SHORT).show()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = "PDF icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "PDF Attached: ${Uri.parse(uri).lastPathSegment}",
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove PDF")
        }
    }
}
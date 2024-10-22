package com.mytaskpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.CategoryType

@Composable
fun CategorySelectionDialog(
    onDismiss: () -> Unit,
    onCategorySelected: (CategoryType) -> Unit,
    onNewCategoryCreated: (String) -> Unit,
    customCategories: List<CategoryType>
) {
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            Column {
                // Predefined categories
                CategoryType.values()
                    .filter { it.type != "COMPLETED" } // Filter out COMPLETED
                    .forEach { category ->
                        CategoryItem(category, onCategorySelected)
                    }

                // Custom categories
                customCategories.forEach { category ->
                    CategoryItem(category, onCategorySelected)
                }

                // New Category option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewCategoryDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add new category")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Category")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Create New Category") },
            text = {
                TextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onNewCategoryCreated(newCategoryName)
                            showNewCategoryDialog = false
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: CategoryType,
    onCategorySelected: (CategoryType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCategorySelected(category) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(category.color))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(category.displayName)
    }
}
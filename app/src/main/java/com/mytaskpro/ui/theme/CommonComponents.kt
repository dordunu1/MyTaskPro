package com.mytaskpro.ui


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.mytaskpro.data.CategoryType

@Composable
fun CategoryDropdown(
    selectedCategory: CategoryType,
    onCategorySelected: (CategoryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true }
        ) {
            Text("Category: ${selectedCategory.displayName}")
            Icon(Icons.Default.ArrowDropDown, "Expand")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CategoryType.values().forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(category.icon, contentDescription = null)
                    }
                )
            }
        }
    }
}
package com.mytaskpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mytaskpro.ui.theme.AppTheme

@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppTheme.values()) { theme ->
                    ThemeItem(theme = theme, onSelect = onThemeSelected)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ThemeItem(theme: AppTheme, onSelect: (AppTheme) -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(getThemeColor(theme))
            .clickable { onSelect(theme) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = theme.name,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(4.dp)
        )
    }
}

fun getThemeColor(theme: AppTheme): Color {
    return when (theme) {
        AppTheme.Default -> Color(0xFF5C9EAD)
        AppTheme.ClassicLight -> Color(0xFF5C9EAD)
        AppTheme.WarmSepia -> Color(0xFFD9534F)
        AppTheme.Dark -> Color(0xFF0A0A0A)
        AppTheme.HighContrast -> Color(0xFFFFD700)
        AppTheme.SoftBlue -> Color(0xFF90CAF9)
        AppTheme.Pink -> Color(0xFFE91E63)
        AppTheme.PaperLight -> Color(0xFFDAB894)
        AppTheme.PaperDark -> Color(0xFFBDAA7E)
    }
}
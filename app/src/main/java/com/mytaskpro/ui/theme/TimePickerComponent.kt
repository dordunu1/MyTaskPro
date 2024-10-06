package com.mytaskpro.ui


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalTime
@Composable
fun StableTimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    var hour by remember { mutableStateOf(0) }
    var minute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column {
                NumberPicker(
                    value = hour,
                    onValueChange = { hour = it },
                    range = 0..23,
                    label = "Hour"
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberPicker(
                    value = minute,
                    onValueChange = { minute = it },
                    range = 0..59,
                    label = "Minute"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime.of(hour, minute))
                onDismissRequest()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    var textValue by remember { mutableStateOf(value.toString().padStart(2, '0')) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(60.dp))
        IconButton(onClick = {
            val newValue = (value - 1).coerceIn(range)
            onValueChange(newValue)
            textValue = newValue.toString().padStart(2, '0')
        }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Decrease")
        }
        TextField(
            value = textValue,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    textValue = it
                    it.toIntOrNull()?.let { num ->
                        if (num in range) {
                            onValueChange(num)
                        }
                    }
                }
            },
            modifier = Modifier.width(60.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
        IconButton(onClick = {
            val newValue = (value + 1).coerceIn(range)
            onValueChange(newValue)
            textValue = newValue.toString().padStart(2, '0')
        }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Increase")
        }
    }
}
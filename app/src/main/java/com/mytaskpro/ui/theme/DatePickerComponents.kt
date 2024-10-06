package com.mytaskpro.ui.theme


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytaskpro.ui.NumberPicker
import java.time.LocalDate

@Composable
fun CustomDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(initialDate.year) }
    var month by remember { mutableStateOf(initialDate.monthValue) }
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column {
                NumberPicker(
                    value = year,
                    onValueChange = { year = it },
                    range = 2000..2100,
                    label = "Year"
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberPicker(
                    value = month,
                    onValueChange = { month = it },
                    range = 1..12,
                    label = "Month"
                )
                Spacer(modifier = Modifier.height(16.dp))
                NumberPicker(
                    value = day,
                    onValueChange = { day = it },
                    range = 1..31,
                    label = "Day"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(LocalDate.of(year, month, day))
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
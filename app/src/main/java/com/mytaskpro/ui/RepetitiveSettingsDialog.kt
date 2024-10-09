import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytaskpro.data.RepetitiveTaskSettings
import com.mytaskpro.ui.DailyRepetitionOptions
import com.mytaskpro.ui.RepetitionType
import com.mytaskpro.ui.EndOption
import com.mytaskpro.ui.EndOptionsSelector
import com.mytaskpro.ui.MonthlyRepetitionOptions
import com.mytaskpro.ui.RepetitionTypeSelector
import com.mytaskpro.ui.WeekdaysRepetitionOption
import com.mytaskpro.ui.WeeklyRepetitionOptions
import com.mytaskpro.ui.YearlyRepetitionOptions

@Composable
fun RepetitiveSettingsDialog(
    currentSettings: RepetitiveTaskSettings?,
    onDismiss: () -> Unit,
    onSave: (RepetitiveTaskSettings?) -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings ?: RepetitiveTaskSettings()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Repetitive Settings") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Repeat:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                RepetitionTypeSelector(
                    selectedType = settings.type,
                    onTypeSelected = { settings = settings.copy(type = it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                when (settings.type) {
                    RepetitionType.ONE_TIME -> {}
                    RepetitionType.DAILY -> DailyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                    RepetitionType.WEEKDAYS -> WeekdaysRepetitionOption(settings, onSettingsChanged = { settings = it })
                    RepetitionType.WEEKLY -> WeeklyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                    RepetitionType.MONTHLY -> MonthlyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                    RepetitionType.YEARLY -> YearlyRepetitionOptions(settings, onSettingsChanged = { settings = it })
                }
                if (settings.type != RepetitionType.ONE_TIME) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EndOptionsSelector(settings, onSettingsChanged = { settings = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(settings) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
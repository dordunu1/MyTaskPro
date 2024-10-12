package com.mytaskpro

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import java.time.LocalTime
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.time.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    isUserSignedIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val userEmail by settingsViewModel.userEmail.collectAsState()
    Log.d("SettingsScreen", "Current user email: $userEmail")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralSettingsSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            NotificationSettingsSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            SyncSection(taskViewModel, settingsViewModel, isUserSignedIn, onGoogleSignIn, onSignOut)
            Spacer(modifier = Modifier.height(16.dp))
            WidgetCustomizationSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            PremiumSubscriptionSection(settingsViewModel) // Replace DataManagementSection with this
            Spacer(modifier = Modifier.height(16.dp))
            FeedbackSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            AboutSection(settingsViewModel)
        }
    }
}

@Composable
fun GeneralSettingsSection(viewModel: SettingsViewModel) {
    SettingsSection(title = "General") {
        SwitchSetting("Dark Mode", viewModel.isDarkMode.collectAsState().value) { viewModel.toggleDarkMode() }
        SwitchSetting("24-Hour Format", viewModel.is24HourFormat.collectAsState().value) { viewModel.toggle24HourFormat() }
        DropdownSetting("Theme", viewModel.currentTheme.collectAsState().value, viewModel.availableThemes.collectAsState().value) { viewModel.setTheme(it) }
        DropdownSetting("Language", viewModel.currentLanguage.collectAsState().value, viewModel.availableLanguages.collectAsState().value) { viewModel.setLanguage(it) }
    }
}

@Composable
fun NotificationSettingsSection(viewModel: SettingsViewModel) {
    SettingsSection(title = "Notifications") {
        SwitchSetting("Task Reminders", viewModel.taskReminders.collectAsState().value) { viewModel.toggleTaskReminders() }
        SwitchSetting("Daily Summary", viewModel.dailySummary.collectAsState().value) { viewModel.toggleDailySummary() }
        if (viewModel.dailySummary.collectAsState().value) {
            TimeSetting("Summary Time", viewModel.dailySummaryTime.collectAsState().value) { viewModel.setDailySummaryTime(it) }
        }
    }
}

@Composable
fun SyncSection(
    taskViewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    isUserSignedIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    showDebugButtons: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val syncStatus by rememberUpdatedState(settingsViewModel.syncStatus.collectAsState().value)
    val lastSyncTime by rememberUpdatedState(settingsViewModel.lastSyncTime.collectAsState().value)
    val isGoogleSyncEnabled by rememberUpdatedState(settingsViewModel.isGoogleSyncEnabled.collectAsState().value)
    val isSyncing by rememberUpdatedState(settingsViewModel.isSyncing.collectAsState().value)
    val userEmail by settingsViewModel.userEmail.collectAsState()

    var forceUpdate by remember { mutableStateOf(0) }

    LaunchedEffect(syncStatus, lastSyncTime, isGoogleSyncEnabled, isSyncing, forceUpdate) {
        Log.d("SyncSection", "Current sync status: $syncStatus")
        Log.d("SyncSection", "Last sync time: $lastSyncTime")
        Log.d("SyncSection", "Is Google Sync enabled: $isGoogleSyncEnabled")
        Log.d("SyncSection", "Is syncing: $isSyncing")
        Log.d("SyncSection", "Force update trigger: $forceUpdate")
        Log.d("SyncSection", "Current user email: $userEmail")
    }

    SettingsSection(title = "Sync") {
        key(syncStatus, lastSyncTime, isGoogleSyncEnabled, isSyncing, forceUpdate) {
            Column {
                if (isUserSignedIn) {
                    userEmail?.let { email ->
                        Text(
                            text = "Signed in as: $email",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    SwitchSetting("Google Sync", isGoogleSyncEnabled) { settingsViewModel.toggleGoogleSync() }
                    if (isGoogleSyncEnabled) {
                        Text(
                            text = "Last synced: ${lastSyncTime ?: "Never"}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        if (isSyncing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            when (syncStatus) {
                                SettingsViewModel.SyncStatus.Success -> {
                                    Text(
                                        "Sync completed successfully",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    LaunchedEffect(Unit) {
                                        delay(3000)
                                        settingsViewModel.resetSyncStatus()
                                    }
                                }
                                SettingsViewModel.SyncStatus.Error -> {
                                    Text(
                                        "Sync failed. Tap to retry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                coroutineScope.launch {
                                                    settingsViewModel.startSync()
                                                    taskViewModel.syncTasksWithFirebase()
                                                    delay(1000)
                                                    settingsViewModel.endSync(true)
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                                    )
                                }
                                SettingsViewModel.SyncStatus.Idle -> {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                settingsViewModel.startSync()
                                                taskViewModel.syncTasksWithFirebase()
                                                delay(1000)
                                                settingsViewModel.endSync(true)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    ) {
                                        Text("Sync Now")
                                    }
                                }
                                SettingsViewModel.SyncStatus.Syncing -> {
                                    // This case is handled by the isSyncing check above
                                }
                            }
                        }
                    }
                    TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Sign Out")
                    }
                } else {
                    TextButton(onClick = onGoogleSignIn, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Sign in with Google")
                    }
                }

                if (showDebugButtons) {
                    TextButton(
                        onClick = {
                            forceUpdate++
                            settingsViewModel.triggerForceRefresh()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Force UI Refresh")
                    }
                    TextButton(
                        onClick = { settingsViewModel.startSync() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Debug: Start Sync")
                    }
                    TextButton(
                        onClick = { settingsViewModel.endSync(true) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Debug: End Sync (Success)")
                    }
                    TextButton(
                        onClick = { settingsViewModel.endSync(false) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Debug: End Sync (Error)")
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetCustomizationSection(viewModel: SettingsViewModel) {
    SettingsSection(title = "Widget Customization") {
        DropdownSetting("Widget Theme", viewModel.widgetTheme.collectAsState().value, viewModel.availableThemes.collectAsState().value) { viewModel.setWidgetTheme(it) }
        SliderSetting("Tasks to Show", viewModel.widgetTaskCount.collectAsState().value, 1f..10f) { viewModel.setWidgetTaskCount(it.toInt()) }
    }
}

@Composable
fun PremiumSubscriptionSection(viewModel: SettingsViewModel) {
    SettingsSection(title = "Premium Subscription") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Premium",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Unlock advanced features and sync across devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = { /* Implement subscription logic */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Subscribe Now")
        }
        TextButton(
            onClick = { /* Implement restore purchases logic */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore Purchases")
        }
    }
}

@Composable
fun FeedbackSection(viewModel: SettingsViewModel) {
    SettingsSection(title = "Feedback") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { viewModel.provideFeedback() }) {
                Text("Provide Feedback")
            }
            TextButton(onClick = { viewModel.reportIssue() }) {
                Text("Report Issue")
            }
        }
    }
}

@Composable
fun AboutSection(viewModel: SettingsViewModel) {
    SettingsSection(title = "About") {
        Text(
            "Version: ${viewModel.appVersion.collectAsState().value}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        TextButton(onClick = { /* Open privacy policy */ }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Privacy Policy")
        }
        TextButton(onClick = { /* Open terms of service */ }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Terms of Service")
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        Divider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DropdownSetting(title: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TimeSetting(title: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    // Implement time picker dialog here
}

@Composable
fun SliderSetting(title: String, value: Int, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            steps = (range.endInclusive - range.start).toInt()
        )
        Text("$value", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PremiumFeatureTeaser(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Icon(Icons.Default.Lock, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("$feature (Premium)", style = MaterialTheme.typography.bodyMedium)
    }
}
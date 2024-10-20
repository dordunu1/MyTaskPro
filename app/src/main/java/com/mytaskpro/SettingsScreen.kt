package com.mytaskpro

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytaskpro.ui.theme.AppTheme
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime


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
            GeneralSettingsSection(settingsViewModel, themeViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            NotificationSettingsSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            SyncSection(taskViewModel, settingsViewModel, isUserSignedIn, onGoogleSignIn, onSignOut)
            Spacer(modifier = Modifier.height(16.dp))
            WidgetCustomizationSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            PremiumSubscriptionSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            FeedbackSection(viewModel = settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            AboutSection(settingsViewModel)
        }
    }
}

@Composable
fun GeneralSettingsSection(viewModel: SettingsViewModel, themeViewModel: ThemeViewModel) {
    var showThemeDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "General") {
        SwitchSetting(
            "Dark Mode",
            viewModel.isDarkMode.collectAsState().value
        ) { isChecked ->
            viewModel.toggleDarkMode()
            // Update the theme in ThemeViewModel
            themeViewModel.setTheme(if (isChecked) AppTheme.Dark else AppTheme.Default)
        }
        SwitchSetting(
            "24-Hour Format",
            viewModel.is24HourFormat.collectAsState().value
        ) { viewModel.toggle24HourFormat() }
        ClickableSetting("Theme") {
            showThemeDialog = true
        }
        SwitchSetting(
            "Status Bar Quick Add",
            viewModel.isStatusBarQuickAddEnabled.collectAsState().value
        ) { viewModel.toggleStatusBarQuickAdd() }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeViewModel.currentTheme.collectAsState().value,
            onThemeSelected = { theme ->
                themeViewModel.setTheme(theme)
                // Update isDarkMode in SettingsViewModel based on the selected theme
                viewModel.setDarkMode(theme == AppTheme.Dark)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppTheme.values()) { theme ->
                    ThemeOption(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelect = { onThemeSelected(theme) }
                    )
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
fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(getThemeColor(theme))
            )
            Text(
                text = theme.name,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun getThemeColor(theme: AppTheme): Color {
    return when (theme) {
        AppTheme.Default -> MaterialTheme.colorScheme.primary
        AppTheme.ClassicLight -> Color(0xFF5C9EAD)
        AppTheme.WarmSepia -> Color(0xFFD9534F)
        AppTheme.Dark -> Color(0xFF0D0E0E)
        AppTheme.HighContrast -> Color(0xFFFFD700)
        AppTheme.SoftBlue -> Color(0xFF90CAF9)
        AppTheme.Pink -> Color(0xFFE91E63)
        AppTheme.PaperLight -> Color(0xFFDAB894)
        AppTheme.PaperDark -> Color(0xFFBDAA7E)
    }
}

@Composable
fun ClickableSetting(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.ChevronRight, contentDescription = null)
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
    var showFeedbackDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "Feedback") {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { showFeedbackDialog = true },
                modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp)
            ) {
                Text("Provide Feedback")
            }
            TextButton(
                onClick = { showFeedbackDialog = true },
                modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp)
            ) {
                Text("Report Issue")
            }
        }
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit, viewModel: SettingsViewModel) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("We Value Your Input!", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                "Your feedback helps us improve MyTaskPro for everyone. We'd love to hear your thoughts, suggestions, or any issues you've encountered.\n\n" +
                        "Please consider leaving a review on the Play Store. It only takes a moment and makes a big difference!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        context.startActivity(viewModel.getPlayStoreIntent())
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(viewModel.getPlayStoreWebIntent())
                    }
                    onDismiss()
                }
            ) {
                Text("Go to Play Store")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Terms of Service",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    """
                    Welcome to MyTaskPro! These Terms of Service govern your use of our app and services. By using MyTaskPro, you agree to these terms.

                    1. Use of the App
                    MyTaskPro is a task management app designed to help you organize and track your tasks. You may use the app for personal or professional purposes in accordance with these terms.

                    2. Account Creation
                    While you can use many features of MyTaskPro without an account, syncing tasks with Google requires signing in with your Google account. This is to ensure your data can be securely stored and synchronized across devices.

                    3. User Responsibilities
                    You are responsible for maintaining the confidentiality of your account information and for all activities that occur under your account. You agree to use the app in compliance with all applicable laws and regulations.

                    4. Data and Privacy
                    We respect your privacy and handle your data in accordance with our Privacy Policy. By using MyTaskPro, you consent to the collection and use of information as detailed in our Privacy Policy.

                    5. Intellectual Property
                    All content and functionality within MyTaskPro, including but not limited to text, graphics, logos, and software, is the property of MyTaskPro or its licensors and is protected by copyright and other intellectual property laws.

                    6. Premium Features
                    MyTaskPro offers both free and premium features. Premium features are available through in-app purchases. Prices and features are subject to change.

                    7. Modifications to the App
                    We reserve the right to modify or discontinue, temporarily or permanently, the app or any features or portions thereof without prior notice.

                    8. Limitation of Liability
                    MyTaskPro is provided "as is" without any warranties. We shall not be liable for any indirect, incidental, special, consequential or punitive damages resulting from your use of the app.

                    9. Governing Law
                    These terms shall be governed by and construed in accordance with the laws of [Your Jurisdiction], without regard to its conflict of law provisions.

                    10. Changes to Terms
                    We may update these terms from time to time. We will notify you of any changes by posting the new Terms of Service on this page.

                    If you have any questions about these Terms of Service, please contact us at support@mytaskpro.com.
                    
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    """
                    At MyTaskPro, we value your privacy and are committed to protecting your personal information. This Privacy Policy explains how we collect, use, and safeguard your data when you use our app.

                    1. Information We Collect

                    a. Task Data: We collect and store the tasks you create, including titles, descriptions, due dates, and completion status.
                    b. Google Account Information: If you choose to sync your tasks, we access your Google account information for authentication purposes.
                    c. Usage Data: We collect anonymous data about how you use the app to improve our services.

                    2. How We Use Your Information

                    a. To provide and maintain the MyTaskPro service.
                    b. To sync your tasks across devices when you enable Google Sync.
                    c. To improve and personalize your experience with the app.
                    d. To communicate with you about app updates or respond to your inquiries.

                    3. Data Storage and Security

                    a. Local Storage: By default, your tasks are stored locally on your device.
                    b. Cloud Storage: If you enable Google Sync, your tasks are also stored securely in your Google account.
                    c. We implement industry-standard security measures to protect your data.

                
                    4. Your Choices

                    a. You can use MyTaskPro without creating an account or syncing to Google.
                    b. You can enable or disable Google Sync at any time in the app settings.
                    c. You can request deletion of your data by contacting us.

                    6. Changes to This Policy

                    We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.

                    7. Contact Us

                    If you have any questions about this Privacy Policy, please contact us at privacy@mytaskpro.com.

                   
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AboutSection(viewModel: SettingsViewModel) {
    var showTermsOfService by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    SettingsSection(title = "About") {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Version: ${viewModel.appVersion.collectAsState().value}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            TextButton(
                onClick = { showPrivacyPolicy = true },
                modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp)
            ) {
                Text("Privacy Policy")
            }
            TextButton(
                onClick = { showTermsOfService = true },
                modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp)
            ) {
                Text("Terms of Service")
            }
        }
    }

    if (showTermsOfService) {
        TermsOfServiceDialog(onDismiss = { showTermsOfService = false })
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
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
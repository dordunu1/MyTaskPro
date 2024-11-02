package com.mytaskpro

import android.app.Activity
import android.content.ActivityNotFoundException
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.mytaskpro.data.TaskPriority
import com.mytaskpro.ui.theme.AppTheme
import com.mytaskpro.ui.theme.VibrantBlue
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
    onSignOut: () -> Unit,
    activity: Activity // Add this parameter
) {
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
            GeneralSettingsSection(settingsViewModel, themeViewModel, activity)
            Spacer(modifier = Modifier.height(16.dp))
            NotificationSettingsSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            SyncSection(taskViewModel, settingsViewModel, isUserSignedIn, onGoogleSignIn, onSignOut, activity = activity)
            Spacer(modifier = Modifier.height(16.dp))
            TaskPrioritySection(settingsViewModel, activity)
            Spacer(modifier = Modifier.height(16.dp))
            PremiumFeaturesSection(settingsViewModel, activity)
            Spacer(modifier = Modifier.height(16.dp))
            PremiumSubscriptionSection(settingsViewModel, activity)
            Spacer(modifier = Modifier.height(16.dp))
            FeedbackSection(viewModel = settingsViewModel, activity = activity)
            Spacer(modifier = Modifier.height(16.dp))
            FAQSection(settingsViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            AboutSection(settingsViewModel)
        }
    }
}
@Composable
fun GeneralSettingsSection(
    viewModel: SettingsViewModel,
    themeViewModel: ThemeViewModel,
    activity: Activity
) {
    val isPremium by viewModel.isPremium.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPremiumThemeDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "General") {
        SwitchSetting(
            "Dark Mode",
            viewModel.isDarkMode.collectAsState().value
        ) { isChecked ->
            viewModel.toggleDarkMode()
            themeViewModel.setTheme(if (isChecked) AppTheme.Dark else AppTheme.Default)
        }
        SwitchSetting(
            "24-Hour Format",
            viewModel.is24HourFormat.collectAsState().value
        ) { viewModel.toggle24HourFormat() }

        if (isPremium) {
            ClickableSetting("Theme") {
                showThemeDialog = true
            }
        } else {
            ClickableSetting("Theme (Premium)") {
                showPremiumThemeDialog = true
            }
        }

        SwitchSetting(
            "Status Bar Quick Add",
            viewModel.isStatusBarQuickAddEnabled.collectAsState().value
        ) { viewModel.toggleStatusBarQuickAdd() }
    }

    if (showThemeDialog && isPremium) {
        themeViewModel.currentTheme.collectAsState().value?.let {
            ThemeSelectionDialog(
                currentTheme = it,
                onThemeSelected = { theme ->
                    themeViewModel.setTheme(theme)
                    viewModel.setDarkMode(theme == AppTheme.Dark)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }
    }

    if (showPremiumThemeDialog && !isPremium) {
        PremiumFeatureDialog(
            feature = "Custom Themes",
            description = "Unlock a variety of beautiful themes to personalize your app experience.",
            onUpgrade = { viewModel.upgradeToPremium(activity) },
            onDismiss = { showPremiumThemeDialog = false }
        )
    }
}

@Composable
fun PremiumFeatureDialog(
    feature: String,
    description: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade to Premium") },
        text = {
            Column {
                Text("$feature is a premium feature.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(description)
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpgrade()
                onDismiss()
            }) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
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
fun getThemeColor(theme: AppTheme, isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
    return when (theme) {
        AppTheme.Default -> if (isDarkTheme) VibrantBlue else VibrantBlue // Use the same color for both light and dark themes
        AppTheme.ClassicLight -> Color(0xFF5C9EAD)
        AppTheme.BeThankful -> Color(0xFFD06A4E)
        AppTheme.EInkTheme -> Color(0xFFF5F5F5)
        AppTheme.WarmSepia -> Color(0xFFD9534F)
        AppTheme.Dark -> Color(0xFF0D0E0E)
        AppTheme.MiddleYellowRed -> Color(0xFFF0AF84)
        AppTheme.SoftBlue -> Color(0xFF90CAF9)
        AppTheme.Pink -> Color(0xFFE91E63)
        AppTheme.MistyMoon -> Color(0xFF696156)
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
    }
}

@Composable
fun SyncSection(
    taskViewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    isUserSignedIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    showDebugButtons: Boolean = false,
    activity: Activity
) {
    val coroutineScope = rememberCoroutineScope()
    val syncStatus by rememberUpdatedState(settingsViewModel.syncStatus.collectAsState().value)
    val lastSyncTime by rememberUpdatedState(settingsViewModel.lastSyncTime.collectAsState().value)
    val isGoogleSyncEnabled by rememberUpdatedState(settingsViewModel.isGoogleSyncEnabled.collectAsState().value)
    val isGoogleCalendarSyncEnabled by rememberUpdatedState(settingsViewModel.isGoogleCalendarSyncEnabled.collectAsState().value)
    val isSyncing by rememberUpdatedState(settingsViewModel.isSyncing.collectAsState().value)
    val userEmail by settingsViewModel.userEmail.collectAsState()
    val isPremium by settingsViewModel.isPremium.collectAsState()

    var forceUpdate by remember { mutableStateOf(0) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncStatus, lastSyncTime, isGoogleSyncEnabled, isGoogleCalendarSyncEnabled, isSyncing, forceUpdate) {
        Log.d("SyncSection", "Current sync status: $syncStatus")
        Log.d("SyncSection", "Last sync time: $lastSyncTime")
        Log.d("SyncSection", "Is Google Sync enabled: $isGoogleSyncEnabled")
        Log.d("SyncSection", "Is Google Calendar Sync enabled: $isGoogleCalendarSyncEnabled")
        Log.d("SyncSection", "Is syncing: $isSyncing")
        Log.d("SyncSection", "Force update trigger: $forceUpdate")
        Log.d("SyncSection", "Current user email: $userEmail")
    }

    SettingsSection(title = "Sync") {
        key(syncStatus, lastSyncTime, isGoogleSyncEnabled, isGoogleCalendarSyncEnabled, isSyncing, forceUpdate) {
            Column {
                if (isUserSignedIn) {
                    userEmail?.let { email ->
                        Text(
                            text = "Signed in as: $email",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (isPremium) {
                        SwitchSetting("Google Sync", isGoogleSyncEnabled) { settingsViewModel.toggleGoogleSync() }
                        SwitchSetting("Google Calendar Sync", isGoogleCalendarSyncEnabled) {
                            settingsViewModel.toggleGoogleCalendarSync(userEmail ?: "")
                        }
                        if (isGoogleSyncEnabled) {
                            SyncStatusSection(
                                syncStatus = syncStatus,
                                lastSyncTime = lastSyncTime,
                                isSyncing = isSyncing,
                                onSyncNow = {
                                    coroutineScope.launch {
                                        settingsViewModel.startSync()
                                        taskViewModel.syncTasksWithFirebase()
                                        delay(1000)
                                        settingsViewModel.endSync(true)
                                    }
                                },
                                onResetSyncStatus = { settingsViewModel.resetSyncStatus() }
                            )
                        }
                    } else {
                        PremiumFeatureTeaser("Google Sync") {
                            showPremiumDialog = true
                        }
                        PremiumFeatureTeaser("Google Calendar Sync") {
                            showPremiumDialog = true
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
                    DebugButtons(
                        onForceRefresh = {
                            forceUpdate++
                            settingsViewModel.triggerForceRefresh()
                        },
                        onStartSync = { settingsViewModel.startSync() },
                        onEndSyncSuccess = { settingsViewModel.endSync(true) },
                        onEndSyncError = { settingsViewModel.endSync(false) }
                    )
                }
            }
        }
    }

    if (showPremiumDialog) {
        PremiumFeatureDialog(
            feature = "Google Sync and Calendar Sync",
            description = "Sync your tasks across devices and integrate with Google Calendar.",
            onUpgrade = { settingsViewModel.upgradeToPremium(activity) },
            onDismiss = { showPremiumDialog = false }
        )
    }
}

@Composable
fun SyncStatusSection(
    syncStatus: SettingsViewModel.SyncStatus,
    lastSyncTime: String?,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onResetSyncStatus: () -> Unit
) {
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
                    onResetSyncStatus()
                }
            }
            SettingsViewModel.SyncStatus.Error -> {
                Text(
                    "Sync failed. Tap to retry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSyncNow() }
                        .padding(vertical = 8.dp)
                )
            }
            SettingsViewModel.SyncStatus.Idle -> {
                TextButton(
                    onClick = onSyncNow,
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

@Composable
fun DebugButtons(
    onForceRefresh: () -> Unit,
    onStartSync: () -> Unit,
    onEndSyncSuccess: () -> Unit,
    onEndSyncError: () -> Unit
) {
    Column {
        TextButton(
            onClick = onForceRefresh,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Force UI Refresh")
        }
        TextButton(
            onClick = onStartSync,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Debug: Start Sync")
        }
        TextButton(
            onClick = onEndSyncSuccess,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Debug: End Sync (Success)")
        }
        TextButton(
            onClick = onEndSyncError,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Debug: End Sync (Error)")
        }
    }
}

@Composable
fun TaskPrioritySection(viewModel: SettingsViewModel, activity: Activity) {
    val isPremium by viewModel.isPremium.collectAsState()
    var showPremiumDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "Task Priority") {
        if (isPremium) {
            SwitchSetting(
                "Enable Task Priorities",
                viewModel.isTaskPriorityEnabled.collectAsState().value
            ) { viewModel.toggleTaskPriority() }

            if (viewModel.isTaskPriorityEnabled.collectAsState().value) {
                DropdownSetting(
                    "Default Priority",
                    viewModel.defaultTaskPriority.collectAsState().value.name,
                    TaskPriority.values().map { it.name }
                ) { viewModel.setDefaultTaskPriority(TaskPriority.valueOf(it)) }
            }
        } else {
            PremiumFeatureTeaser("Task Priorities") {
                showPremiumDialog = true
            }
        }
    }

    if (showPremiumDialog) {
        PremiumFeatureDialog(
            feature = "Task Priorities",
            description = "Organize your tasks with customizable priority levels to focus on what matters most.",
            onUpgrade = { viewModel.upgradeToPremium(activity) },
            onDismiss = { showPremiumDialog = false }
        )
    }
}

@Composable
fun PremiumFeaturesSection(viewModel: SettingsViewModel, activity: Activity) {
    val isPremium by viewModel.isPremium.collectAsState()
    var showTaskSummaryGraphDialog by remember { mutableStateOf(false) }
    var showAchievementBadgesDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "Premium Features") {
        if (isPremium) {
            TaskSummaryGraphSetting(viewModel)
            AchievementBadgesSetting(viewModel)
        } else {
            PremiumFeatureTeaser("Task Summary Graph") {
                showTaskSummaryGraphDialog = true
            }
            PremiumFeatureTeaser("Achievement Badges") {
                showAchievementBadgesDialog = true
            }
        }
    }

    if (showTaskSummaryGraphDialog) {
        PremiumFeatureDialog(
            feature = "Task Summary Graph",
            description = "Visualize your task completion trends and productivity patterns with interactive graphs.",
            onUpgrade = { viewModel.upgradeToPremium(activity) },
            onDismiss = { showTaskSummaryGraphDialog = false }
        )
    }

    if (showAchievementBadgesDialog) {
        PremiumFeatureDialog(
            feature = "Achievement Badges",
            description = "Earn badges for completing tasks and reaching milestones to stay motivated.",
            onUpgrade = { viewModel.upgradeToPremium(activity) },
            onDismiss = { showAchievementBadgesDialog = false }
        )
    }
}

@Composable
fun TaskSummaryGraphSetting(viewModel: SettingsViewModel) {
    val isTaskSummaryGraphEnabled by viewModel.isTaskSummaryGraphEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        SwitchSetting(
            title = "Enable Task Summary Graph",
            checked = isTaskSummaryGraphEnabled,
            onCheckedChange = { viewModel.toggleTaskSummaryGraph() }
        )
        if (isTaskSummaryGraphEnabled) {
            Text(
                text = "Task summary graph is enabled. You can view it on the dashboard.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun AchievementBadgesSetting(viewModel: SettingsViewModel) {
    val isAchievementBadgesEnabled by viewModel.isAchievementBadgesEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        SwitchSetting(
            title = "Enable Achievement Badges",
            checked = isAchievementBadgesEnabled,
            onCheckedChange = { viewModel.toggleAchievementBadges() }
        )
        if (isAchievementBadgesEnabled) {
            Text(
                text = "Achievement badges are enabled. Complete tasks to earn badges!",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PremiumFeatureTeaser(feature: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Premium Feature",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("$feature (Premium)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Learn More",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PremiumSubscriptionSection(viewModel: SettingsViewModel, activity: Activity) {
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current

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
                    text = if (isPremium) "Premium Member" else "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isPremium) "Enjoy all premium features" else "Unlock advanced features and sync across devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isPremium) {
            Button(
                onClick = {
                    viewModel.upgradeToPremium(context as Activity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Upgrade Now")
            }
        }
        if (!isPremium) {
            TextButton(
                onClick = { /* Implement restore purchases logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Purchases")
            }
        }
    }
}

@Composable
fun FeedbackSection(viewModel: SettingsViewModel, activity: Activity) {
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
fun FAQSection(viewModel: SettingsViewModel) {
    var showFAQDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "Help & FAQ") {
        TextButton(
            onClick = { showFAQDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View FAQ")
        }
    }

    if (showFAQDialog) {
        FAQDialog(onDismiss = { showFAQDialog = false })
    }
}

@Composable
private fun FAQDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Notification Troubleshooting Guide",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Introduction
                Text(
                    "To ensure reliable notifications, please configure your device settings:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Samsung Section
                Text(
                    "Samsung Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    """
                    1. Go to Settings > Apps > MyTaskPro
                    2. Select Battery
                    3. Change from "Restricted" to "Unrestricted"
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )

                // Realme/OPPO Section
                Text(
                    "Realme/OPPO Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    """
                    1. Go to Settings > Apps > MyTaskPro
                    2. Select Battery Usage
                    3. Enable these settings:
                       • Allow foreground activity
                       • Allow background activity
                       • Allow auto launch
                       • Allow this app to launch other apps or services
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )

                // Other Devices Section
                Text(
                    "Other Android Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    """
                    1. Go to Settings > Apps > MyTaskPro
                    2. Look for Battery settings
                    3. Disable any power saving restrictions
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )

                // Note
                Text(
                    "Note: After changing these settings, you may need to restart the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                // Close Button
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
private fun FAQItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium
        )
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun AboutSection(viewModel: SettingsViewModel) {
    var showTermsOfService by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val versionInfo = viewModel.getVersionInfo()

    SettingsSection(title = "About") {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Version ${versionInfo.versionName} (${versionInfo.versionCode})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            TextButton(
                onClick = { showPrivacyPolicy = true },
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 4.dp)
            ) {
                Text("Privacy Policy")
            }
            TextButton(
                onClick = { showTermsOfService = true },
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 4.dp)
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
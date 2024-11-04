package com.mytaskpro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.mytaskpro.ui.theme.MyTaskProTheme
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import com.mytaskpro.data.CategoryType
import com.mytaskpro.ui.AddTaskDialog
import com.mytaskpro.ui.AppNavigation
import com.mytaskpro.ui.CategorySelectionDialog
import com.mytaskpro.ui.theme.AppTheme
import com.mytaskpro.ui.viewmodel.AIRecommendationViewModel
import com.mytaskpro.utils.StatusBarNotificationManager
import com.mytaskpro.utils.ThemeUtils
import com.mytaskpro.utils.TimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope
import com.mytaskpro.billing.BillingManager


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val aiRecommendationViewModel: AIRecommendationViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private val billingManager by lazy { BillingManager(this) }


    @Inject
    lateinit var statusBarNotificationManager: StatusBarNotificationManager

    companion object {
        private const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1001
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Check if Calendar permission was granted
            val hasCalendarPermission = GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR))

            taskViewModel.signInWithGoogle(account) { authResult ->
                if (authResult != null && authResult.user != null) {
                    Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Updating email to: ${account.email}")
                    settingsViewModel.updateSignedInEmail(account.email)

                    if (hasCalendarPermission) {
                        // Calendar permission granted, start sync
                        account.email?.let { taskViewModel.startGoogleCalendarSync(it) }
                    } else {
                        // Calendar permission not granted, you may want to inform the user
                        Toast.makeText(this, "Calendar permission not granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateQuickAddNotification() {
        lifecycleScope.launch {
            taskViewModel.getTodaysTasks().collect { todaysTasks ->
                statusBarNotificationManager.showQuickAddNotification(
                    taskCountForToday = todaysTasks.size,
                    tasks = todaysTasks
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        taskViewModel.syncTasksWithFirebase()
        updateQuickAddNotification()
    }

    override fun onPause() {
        super.onPause()
        taskViewModel.saveTasksLocally()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            settingsViewModel.is24HourFormat.collect { is24Hour ->
                TimeUtils.setUse24HourFormat(is24Hour)
            }
        }

        lifecycleScope.launch {
            taskViewModel.getTodaysTasks().collect { todaysTasks ->
                val taskCount = todaysTasks.size
                statusBarNotificationManager.showQuickAddNotification(
                    taskCountForToday = taskCount,
                    tasks = todaysTasks
                )
            }
        }
        updateQuickAddNotification()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .requestScopes(Scope(CalendarScopes.CALENDAR_EVENTS))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("MainActivity", "Firebase Auth State Changed: User signed in, email: ${user.email}")
                settingsViewModel.updateSignedInEmail(user.email)
            } else {
                Log.d("MainActivity", "Firebase Auth State Changed: User signed out")
                settingsViewModel.updateSignedInEmail(null)
            }
        }

        setContent {
            val currentTheme by themeViewModel.currentTheme.collectAsState()

            if (currentTheme == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                MyTaskProApp(
                    taskViewModel = taskViewModel,
                    aiRecommendationViewModel = aiRecommendationViewModel,
                    themeViewModel = themeViewModel,
                    settingsViewModel = settingsViewModel,
                    activity = this // Pass the activity here
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "SHOW_CATEGORY_SELECTION", "ADD_TASK_ACTION" -> {
                taskViewModel.showCategorySelectionDialog()
            }
            "SNOOZE_REMINDER" -> {
                val taskId = intent.getIntExtra("taskId", -1)
                if (taskId != -1) {
                    val snoozeDuration = intent.getLongExtra("snoozeDuration", 15 * 60 * 1000)
                    taskViewModel.snoozeTask(taskId, snoozeDuration)
                }
            }
            "QUICK_ADD_TASK" -> {
                taskViewModel.showCategorySelectionDialog()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyTaskProApp(
        themeViewModel: ThemeViewModel = viewModel(),
        taskViewModel: TaskViewModel = viewModel(),
        aiRecommendationViewModel: AIRecommendationViewModel = viewModel(),
        settingsViewModel: SettingsViewModel = viewModel(),
        activity: Activity // Add this parameter
    ) {
        val currentTheme by themeViewModel.currentTheme.collectAsState()
        val isUserSignedIn by taskViewModel.isUserSignedIn.collectAsState()
        val isDarkTheme = ThemeUtils.isDarkTheme(settingsViewModel)
        val showCategorySelection by taskViewModel.showCategorySelection.collectAsState()
        val showAddTaskDialog by taskViewModel.showAddTaskDialog.collectAsState()
        var selectedCategory by remember { mutableStateOf<CategoryType?>(null) }

        MyTaskProTheme(
            darkTheme = isDarkTheme,
            appTheme = currentTheme ?: AppTheme.Default
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                NotificationPermissionHandler()
                AppNavigation(
                    taskViewModel = taskViewModel,
                    themeViewModel = themeViewModel,
                    settingsViewModel = settingsViewModel,
                    aiRecommendationViewModel = aiRecommendationViewModel,
                    isUserSignedIn = isUserSignedIn,
                    onGoogleSignIn = { signIn() },
                    onSignOut = {
                        taskViewModel.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            Toast.makeText(this@MainActivity, "Signed out", Toast.LENGTH_SHORT)
                                .show()
                            Log.d("MainActivity", "User signed out, updating email to null")
                            settingsViewModel.updateSignedInEmail(email = null)
                        }
                    },
                    activity = this@MainActivity,
                    billingManager = billingManager,
                    onSettingsClick = { /* Implement if needed */ }
                )
                if (showCategorySelection) {
                    CategorySelectionDialog(
                        onDismiss = { taskViewModel.hideCategorySelectionDialog() },
                        onCategorySelected = { category ->
                            selectedCategory = category
                            taskViewModel.hideCategorySelectionDialog()
                            taskViewModel.showAddTaskDialog()
                        },
                        onNewCategoryCreated = { newCategoryName ->
                            taskViewModel.createCustomCategory(newCategoryName)
                        },
                        customCategories = taskViewModel.customCategories.collectAsState().value
                    )
                }

                if (showAddTaskDialog) {
                    selectedCategory?.let {
                        AddTaskDialog(
                            category = it,
                            onDismiss = {
                                taskViewModel.hideAddTaskDialog()
                                selectedCategory = null
                            },
                            onTaskAdded = { title, description, dueDate, reminderTime, notifyOnDueDate, repetitiveSettings, priority ->
                                taskViewModel.addTask(
                                    title = title,
                                    description = description,
                                    category = selectedCategory ?: CategoryType.WORK,
                                    dueDate = dueDate,
                                    reminderTime = reminderTime,
                                    notifyOnDueDate = notifyOnDueDate,
                                    repetitiveSettings = repetitiveSettings,
                                    priority = priority
                                )
                                taskViewModel.hideAddTaskDialog()
                                selectedCategory = null
                            },
                            settingsViewModel = settingsViewModel,
                            activity = this@MainActivity
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun NotificationPermissionHandler() {
        var showDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val notificationManager = NotificationManagerCompat.from(this@MainActivity)
            if (!notificationManager.areNotificationsEnabled()) {
                showDialog = true
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enable Notifications") },
                text = { Text("Notifications are important for task reminders. Would you like to enable them?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                        startActivity(intent)
                    }) {
                        Text("Enable")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Not Now")
                    }
                }
            )
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory("android.intent.category.DEFAULT")
                    data = Uri.parse("package:${applicationContext.packageName}")
                }
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
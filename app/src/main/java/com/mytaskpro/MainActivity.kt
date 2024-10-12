package com.mytaskpro

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mytaskpro.ui.theme.MyTaskProTheme
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import com.mytaskpro.ui.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1001
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            taskViewModel.signInWithGoogle(account) { authResult ->
                if (authResult != null && authResult.user != null) {
                    Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            MyTaskProApp()
        }

        handleIntent(intent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyTaskProApp(
        themeViewModel: ThemeViewModel = viewModel(),
        taskViewModel: TaskViewModel = viewModel()
    ) {
        val currentTheme by themeViewModel.currentTheme.collectAsState()
        val isUserSignedIn by taskViewModel.isUserSignedIn.collectAsState()

        MyTaskProTheme(appTheme = currentTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                NotificationPermissionHandler()
                AppNavigation(
                    taskViewModel = taskViewModel,
                    themeViewModel = themeViewModel,
                    isUserSignedIn = isUserSignedIn,
                    onGoogleSignIn = { signIn() },
                    onSignOut = {
                        taskViewModel.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            Toast.makeText(this@MainActivity, "Signed out", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "COMPLETE_TASK" -> {
                val taskId = intent.getIntExtra("taskId", -1)
                if (taskId != -1) {
                    taskViewModel.updateTaskCompletion(taskId, true)
                }
            }
            "SNOOZE_REMINDER" -> {
                val taskId = intent.getIntExtra("taskId", -1)
                if (taskId != -1) {
                    val snoozeDuration = intent.getLongExtra("snoozeDuration", 15 * 60 * 1000)
                    taskViewModel.snoozeTask(taskId, snoozeDuration)
                }
            }
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
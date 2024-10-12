package com.mytaskpro

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mytaskpro.viewmodel.TaskViewModel
import com.mytaskpro.viewmodel.ThemeViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit
) {
    var isSyncing by remember { mutableStateOf(false) }
    var syncComplete by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            taskViewModel.signInWithGoogle(account) { authResult ->
                if (authResult != null) {
                    isSyncing = true
                    syncComplete = false
                    taskViewModel.viewModelScope.launch {
                        taskViewModel.syncTasksWithFirebase()
                        isSyncing = false
                        syncComplete = true
                        Log.d("SettingsScreen", "Sync completed")
                    }
                } else {
                    Log.e("SettingsScreen", "Google sign-in failed")
                }
            }
        } catch (e: ApiException) {
            Log.e("SettingsScreen", "Google sign-in failed", e)
        }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection(title = "Account") {
                SettingsButton(text = "Sync with Google") {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                }
            }

            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            if (syncComplete) {
                Text("Sync completed successfully", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    taskViewModel: TaskViewModel,
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit,
    isUserSignedIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by taskViewModel.currentUser.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBackClick) {
            Text("Back")
        }

        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Account") {
            if (isUserSignedIn) {
                Text("Signed in as: ${currentUser?.email ?: "Unknown"}")
                Button(onClick = onSignOut) {
                    Text("Sign Out")
                }
                Button(onClick = {
                    coroutineScope.launch {
                        taskViewModel.syncTasksWithFirebase()
                        Toast.makeText(context, "Syncing tasks...", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Sync Tasks")
                }
            } else {
                Button(onClick = onGoogleSignIn) {
                    Text("Sign In with Google")
                }
            }
        }

        // You can add more sections here as needed
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(content = content)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
    Spacer(modifier = Modifier.height(8.dp))
}
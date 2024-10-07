package com.mytaskpro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onSyncClick: () -> Unit,
    onBackupClick: () -> Unit,
    onProSubscriptionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = onSyncClick) {
            Text("Sync with Gmail")
        }

        Button(onClick = onBackupClick) {
            Text("Backup")
        }

        Button(onClick = onProSubscriptionClick) {
            Text("Pro Subscription")
        }

        // Add more profile-related content here
    }
}
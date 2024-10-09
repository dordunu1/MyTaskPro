package com.mytaskpro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var enablePushNotifications by remember { mutableStateOf(false) }
    var enableCalendarIntegration by remember { mutableStateOf(false) }
    var isPremium by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            Text("Task Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { /* TODO: Implement priority levels */ }) {
                Text("Set Task Priority Levels")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { /* TODO: Implement task sharing */ }) {
                Text("Task Sharing Options")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Premium Features", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Premium Status")
                Switch(
                    checked = isPremium,
                    onCheckedChange = { isPremium = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { /* TODO: Implement upgrade to premium */ },
                enabled = !isPremium
            ) {
                Text(if (isPremium) "You are a Premium User" else "Upgrade to Premium")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { /* TODO: Implement backup */ }) {
                Text("Backup Data")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { /* TODO: Implement restore */ }) {
                Text("Restore Data")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Integrations", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Calendar Integration")
                Switch(
                    checked = enableCalendarIntegration,
                    onCheckedChange = { enableCalendarIntegration = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Notifications", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Push Notifications")
                Switch(
                    checked = enablePushNotifications,
                    onCheckedChange = { enablePushNotifications = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Reports", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { /* TODO: Implement productivity reports */ }) {
                Text("View Productivity Reports")
            }
        }
    }
}
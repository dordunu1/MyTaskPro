package com.mytaskpro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mytaskpro.data.Badge
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun BadgeIcon(badge: Badge, onClick: () -> Unit) {
    Icon(
        imageVector = when (badge) {
            Badge.BRONZE -> Icons.Default.Star
            Badge.SILVER -> Icons.Default.Star
            Badge.GOLD -> Icons.Default.Star
            Badge.DIAMOND -> Icons.Default.Star
            Badge.NONE -> Icons.Default.StarBorder
        },
        contentDescription = "Current Badge",
        tint = when (badge) {
            Badge.BRONZE -> Color(0xFFCD7F32)
            Badge.SILVER -> Color.LightGray
            Badge.GOLD -> Color(0xFFFFD700)
            Badge.DIAMOND -> Color(0xFFB9F2FF)
            Badge.NONE -> Color.Gray
        },
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
fun BadgeAchievementPopup(badge: Badge, onDismiss: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }
    var startConfetti by remember { mutableStateOf(false) }

    if (showDialog) {
        Dialog(onDismissRequest = {
            showDialog = false
            onDismiss()
        }) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Confetti (unchanged)
                KonfettiView(
                    parties = listOf(
                        Party(
                            speed = 0f,
                            maxSpeed = 30f,
                            damping = 0.9f,
                            spread = 360,
                            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def).map { Color(it).toArgb() },
                            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                            position = Position.Relative(0.5, 0.3)
                        )
                    ),
                    modifier = Modifier.fillMaxSize()
                )

                // Badge achievement card
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .scale(animateFloatAsState(if (startConfetti) 1f else 0.8f).value),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🎉 Incredible Achievement! 🎉",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "You've unlocked the",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            badge.displayName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Badge!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            getMotivationalMessage(badge),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Keep Rocking!", fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        LaunchedEffect(showDialog) {
            startConfetti = true
        }
    }
}

fun getMotivationalMessage(badge: Badge): String {
    return when (badge) {
        Badge.BRONZE -> "You're off to a fantastic start! Keep up the great work and watch your productivity soar!🥉"
        Badge.SILVER -> "Impressive progress! You're building momentum and achieving great things. The sky's the limit!🥈"
        Badge.GOLD -> "Outstanding performance! Your dedication is truly inspiring. You're a productivity powerhouse!🥇"
        Badge.DIAMOND -> "Phenomenal achievement! You've reached the pinnacle of productivity. You're unstoppable! 💎"
        Badge.NONE -> "Every step counts! Your journey to productivity mastery has begun. Exciting times ahead!"
    }
}
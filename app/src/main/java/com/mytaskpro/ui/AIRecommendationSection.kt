package com.mytaskpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mytaskpro.ai.RecommendationModel
import com.mytaskpro.ai.RecommendationType
import com.mytaskpro.data.CategoryType
import com.mytaskpro.ui.viewmodel.AIRecommendationViewModel
import java.util.Locale

@Composable
fun AIRecommendationSection(viewModel: AIRecommendationViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    val recommendations by viewModel.recommendations.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Improvement Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            if (recommendations.isEmpty()) {
                Text("No recommendations available yet.")
            } else {
                Column {
                    recommendations.forEach { recommendation ->
                        RecommendationItem(recommendation)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationItem(recommendation: RecommendationModel) {
    val backgroundColor = remember(recommendation.type) {
        when (recommendation.type) {
            RecommendationType.RESCHEDULE -> Color(0xFFE57373)
            RecommendationType.BREAK_DOWN -> Color(0xFF81C784)
            RecommendationType.PRIORITIZE -> Color(0xFF64B5F6)
            RecommendationType.DELEGATE -> Color(0xFFFFD54F)
            RecommendationType.FOCUS -> Color(0xFF9575CD)
            RecommendationType.MOTIVATION -> Color(0xFFFF8A65)
            RecommendationType.INSIGHT -> Color(0xFF4DB6AC)
            RecommendationType.STRATEGY -> Color(0xFFBA68C8)
            RecommendationType.BALANCE -> Color(0xFF4FC3F7)
            RecommendationType.OPTIMIZE -> Color(0xFFAED581)
        }.copy(alpha = 0.2f)
    }

    val contentColor = MaterialTheme.colorScheme.onSurface

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = recommendation.type.name.capitalize().replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.9f)
            )
        }
    }
}

fun String.capitalize() = this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(
    Locale.getDefault()) else it.toString() }
@Composable
private fun PremiumFeatureTeaser() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Unlock AI Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Upgrade to premium for personalized task management suggestions!",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { /* Handle upgrade action */ },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Upgrade Now")
            }
        }
    }
}
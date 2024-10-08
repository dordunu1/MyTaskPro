package com.mytaskpro.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.gson.annotations.SerializedName
import kotlin.random.Random

sealed class CategoryType(
    @SerializedName("type") open val type: String,
    @SerializedName("displayName") open val displayName: String,
    @Transient open val icon: ImageVector,
    @SerializedName("color") open val color: Color
) {
    object WORK : CategoryType("WORK", "Work", Icons.Default.Work, Color(0xFF4CAF50))
    object SCHOOL : CategoryType("SCHOOL", "School", Icons.Default.School, Color(0xFF2196F3))
    object SOCIAL : CategoryType("SOCIAL", "Social", Icons.Default.People, Color(0xFFE91E63))
    object CRYPTO : CategoryType("CRYPTO", "Crypto", Icons.Default.CurrencyBitcoin, Color(0xFFFFC107))
    object HEALTH : CategoryType("HEALTH", "Health", Icons.Default.Favorite, Color(0xFF9C27B0))
    object MINDFULNESS : CategoryType("MINDFULNESS", "Mindfulness", Icons.Default.SelfImprovement, Color(0xFF00BCD4))
    object INVOICES : CategoryType("INVOICES", "Invoices", Icons.Default.Receipt, Color(0xFFFF5722))
    object COMPLETED : CategoryType("COMPLETED", "Completed", Icons.Default.CheckCircle, Color(0xFF795548))

    data class Custom(
        @SerializedName("customDisplayName") override val displayName: String,
        @SerializedName("customColor") override val color: Color = generateRandomColor()
    ) : CategoryType("CUSTOM", displayName, Icons.Default.Label, color)

    companion object {
        fun values(): List<CategoryType> = listOf(WORK, SCHOOL, SOCIAL, CRYPTO, HEALTH, MINDFULNESS, INVOICES, COMPLETED)

        fun generateRandomColor(): Color {
            return Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat(),
                alpha = 1f
            )
        }
    }
}
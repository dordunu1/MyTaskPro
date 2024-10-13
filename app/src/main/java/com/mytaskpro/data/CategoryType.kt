package com.mytaskpro.data

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.firestore.Exclude
import kotlin.random.Random

data class CategoryType(
    val type: String = "",
    val displayName: String = "",
    @get:Exclude val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Label,
    val color: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CategoryType
        return type == other.type && displayName == other.displayName
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + displayName.hashCode()
        return result
    }
    companion object {
        val WORK = CategoryType("WORK", "Work", Icons.Default.Work, Color(0xFF4CAF50).toArgb())
        val SCHOOL = CategoryType("SCHOOL", "School", Icons.Default.School, Color(0xFF2196F3).toArgb())
        val SOCIAL = CategoryType("SOCIAL", "Social", Icons.Default.People, Color(0xFFE91E63).toArgb())
        val CRYPTO = CategoryType("CRYPTO", "Crypto", Icons.Default.CurrencyBitcoin, Color(0xFFFFC107).toArgb())
        val HEALTH = CategoryType("HEALTH", "Health", Icons.Default.Favorite, Color(0xFF9C27B0).toArgb())
        val MINDFULNESS = CategoryType("MINDFULNESS", "Mindfulness", Icons.Default.SelfImprovement, Color(0xFF00BCD4).toArgb())
        val INVOICES = CategoryType("INVOICES", "Invoices", Icons.Default.Receipt, Color(0xFFFF5722).toArgb())
        val COMPLETED = CategoryType("COMPLETED", "Completed", Icons.Default.CheckCircle, Color(0xFF795548).toArgb())

        fun values(): List<CategoryType> = listOf(WORK, SCHOOL, SOCIAL, CRYPTO, HEALTH, MINDFULNESS, INVOICES, COMPLETED)

        fun generateRandomColor(): Int {
            return Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat(),
                alpha = 1f
            ).toArgb()
        }

        fun fromString(type: String): CategoryType {
            return values().find { it.type == type } ?: CategoryType(type, type)
        }
    }

    fun toCustom(): CategoryType {
        return CategoryType("CUSTOM", displayName, Icons.Default.Label, color)
    }
}
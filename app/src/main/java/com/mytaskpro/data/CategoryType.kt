package com.mytaskpro.data

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.gson.annotations.SerializedName

sealed class CategoryType(
    @SerializedName("type") open val type: String,
    @SerializedName("displayName") open val displayName: String,
    @Transient open val icon: ImageVector
) {
    object WORK : CategoryType("WORK", "Work", Icons.Default.Work)
    object SCHOOL : CategoryType("SCHOOL", "School", Icons.Default.School)
    object SOCIAL : CategoryType("SOCIAL", "Social", Icons.Default.People)
    object CRYPTO : CategoryType("CRYPTO", "Crypto", Icons.Default.CurrencyBitcoin)
    object HEALTH : CategoryType("HEALTH", "Health", Icons.Default.Favorite)
    object MINDFULNESS : CategoryType("MINDFULNESS", "Mindfulness", Icons.Default.SelfImprovement)
    object INVOICES : CategoryType("INVOICES", "Invoices", Icons.Default.Receipt)
    object COMPLETED : CategoryType("COMPLETED", "Completed", Icons.Default.CheckCircle)

    data class Custom(
        @SerializedName("customDisplayName") override val displayName: String
    ) : CategoryType("CUSTOM", displayName, Icons.Default.Label)

    companion object {
        fun values(): List<CategoryType> = listOf(WORK, SCHOOL, SOCIAL, CRYPTO, HEALTH, MINDFULNESS, INVOICES, COMPLETED)
    }
}
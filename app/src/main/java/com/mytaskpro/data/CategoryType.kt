package com.mytaskpro.data

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.gson.annotations.SerializedName

sealed class CategoryType(
    @SerializedName("displayName") open val displayName: String,
    @Transient open val icon: ImageVector
) {
    object WORK : CategoryType("Work", Icons.Default.Work)
    object SCHOOL : CategoryType("School", Icons.Default.School)
    object SOCIAL : CategoryType("Social", Icons.Default.People)
    object CRYPTO : CategoryType("Crypto", Icons.Default.CurrencyBitcoin)
    object HEALTH : CategoryType("Health", Icons.Default.Favorite)
    object MINDFULNESS : CategoryType("Mindfulness", Icons.Default.SelfImprovement)
    object INVOICES : CategoryType("Invoices", Icons.Default.Receipt)
    object COMPLETED : CategoryType("Completed", Icons.Default.CheckCircle)

    object UNKNOWN : CategoryType("Unknown", Icons.Default.Help)


    data class Custom(
        @SerializedName("customDisplayName") override val displayName: String
    ) : CategoryType(displayName, Icons.Default.Label)

    companion object {
        fun values(): List<CategoryType> = listOf(WORK, SCHOOL, SOCIAL, CRYPTO, HEALTH, MINDFULNESS, INVOICES)
    }
}
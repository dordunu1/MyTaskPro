package com.mytaskpro.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class CategoryType(val displayName: String, val icon: ImageVector) {
    WORK("Work", Icons.Default.Work),
    SCHOOL("School", Icons.Default.School),
    SOCIAL("Social", Icons.Default.People),
    CRYPTO("Crypto", Icons.Default.CurrencyBitcoin),
    HEALTH("Health", Icons.Default.Favorite),
    MINDFULNESS("Mindfulness", Icons.Default.SelfImprovement),
    INVOICES("Invoices", Icons.Default.Receipt),
    COMPLETED("Completed", Icons.Default.CheckCircle)// Added new category
}
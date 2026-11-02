package com.example.catatrapi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val showQuickTransactionFab: Boolean,
) {
    DASHBOARD(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        showQuickTransactionFab = true,
    ),
    CASHFLOW(
        route = "cashflow",
        title = "Cashflow",
        icon = Icons.Default.AccountBalanceWallet,
        showQuickTransactionFab = true,
    ),
    GOALS_WEALTH(
        route = "goals_wealth",
        title = "Goals & Wealth",
        icon = Icons.Default.Savings,
        showQuickTransactionFab = false,
    ),
    ANALYTICS(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.Analytics,
        showQuickTransactionFab = false,
    ),
}

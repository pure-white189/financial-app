package com.example.myapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", R.string.nav_home, Icons.Default.Home)
    object Debt : BottomNavItem("debt", R.string.nav_debt, Icons.Default.AccountBalance)
    object Saving : BottomNavItem("saving", R.string.nav_saving, Icons.Default.Savings)
    object Analysis : BottomNavItem("analysis", R.string.nav_analysis, Icons.Default.PieChart)
}
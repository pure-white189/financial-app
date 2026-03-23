package com.example.myapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "首页", Icons.Default.Home)
    object Debt : BottomNavItem("debt", "借贷", Icons.Default.AccountBalance)
    object Saving : BottomNavItem("saving", "储蓄", Icons.Default.Savings)
    object Analysis : BottomNavItem("analysis", "分析", Icons.Default.PieChart)
}
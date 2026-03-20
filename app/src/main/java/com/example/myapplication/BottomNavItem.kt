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
    object Record : BottomNavItem("record", "记账", Icons.Default.Add)
    object Debt : BottomNavItem("debt", "借贷", Icons.Default.AccountBalance)
    object Saving : BottomNavItem("saving", "储蓄", Icons.Default.Savings)
    object Analysis : BottomNavItem("analysis", "分析", Icons.Default.PieChart)
    object Settings : BottomNavItem("settings", "设置", Icons.Default.Settings)
}
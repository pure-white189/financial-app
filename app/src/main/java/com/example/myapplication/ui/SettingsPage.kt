package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.data.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentBudget: Double?,
    onBudgetChange: (Double?) -> Unit,
    viewModel: ExpenseViewModel,
    currentAlertThreshold: Double?,
    onAlertThresholdChange: (Double?) -> Unit,
    showPersistentNotification: Boolean,
    onPersistentNotificationChange: (Boolean) -> Unit,
    requireDeleteConfirm: Boolean,
    onRequireDeleteConfirmChange: (Boolean) -> Unit,
    onBack: () -> Unit = {},
    onNavigateToExport: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ===== 外观设置 =====
        SettingsSectionTitle(title = "外观")

        SettingsCard {
            // 深色模式
            ThemeSettingItem(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

// ===== 预算设置 =====
        SettingsSectionTitle(title = "预算")

        var showBudgetDialog by remember { mutableStateOf(false) }
        var showAlertDialog by remember { mutableStateOf(false) }

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.AccountBalance,
                title = "月度预算",
                subtitle = if (currentBudget != null) "¥${"%.2f".format(currentBudget)}" else "未设置",
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = { showBudgetDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))  // 添加分隔线

            SettingsItem(  // 添加消费提醒
                icon = Icons.Default.NotificationsActive,
                title = "消费提醒",
                subtitle = if (currentAlertThreshold != null)
                    "单笔超过 ¥${"%.0f".format(currentAlertThreshold)} 时提醒"
                else
                    "未设置",
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = { showAlertDialog = true }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

// 常驻通知开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPersistentNotificationChange(!showPersistentNotification) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "常驻通知",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "状态栏显示预算进度",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = showPersistentNotification,
                onCheckedChange = onPersistentNotificationChange
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRequireDeleteConfirmChange(!requireDeleteConfirm) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Swipe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "滑动删除确认",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "防误触，滑动删除时弹出确认框",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = requireDeleteConfirm,
                onCheckedChange = onRequireDeleteConfirmChange
            )
        }

// 消费提醒设置对话框
        if (showAlertDialog) {
            ExpenseAlertDialog(
                currentThreshold = currentAlertThreshold,
                onDismiss = { showAlertDialog = false },
                onConfirm = { newThreshold ->
                    onAlertThresholdChange(newThreshold)
                    showAlertDialog = false
                }
            )
        }



// 预算设置对话框
        if (showBudgetDialog) {
            BudgetSettingDialog(
                currentBudget = currentBudget,
                onDismiss = { showBudgetDialog = false },
                onConfirm = { newBudget ->
                    onBudgetChange(newBudget)
                    showBudgetDialog = false
                }
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

// ===== 数据管理 =====
        SettingsSectionTitle(title = "数据管理")

        var showDataManagementDialog by remember { mutableStateOf(false) }

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "数据管理",
                subtitle = "查看数据统计、清除数据",
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = { showDataManagementDialog = true }
            )
        }

// 数据管理对话框
        if (showDataManagementDialog) {
            DataManagementDialog(
                viewModel = viewModel,
                onDismiss = { showDataManagementDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

// ===== 数据导出 =====
        SettingsSectionTitle(title = "数据导出")

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = "导出数据",
                subtitle = "导出消费记录为 CSV 格式",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToExport
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 关于 =====
        SettingsSectionTitle(title = "关于")

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "应用版本",
                subtitle = "1.0.0",
                showArrow = false
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsItem(
                icon = Icons.Default.Code,
                title = "技术栈",
                subtitle = "Kotlin + Jetpack Compose + Room",
                showArrow = false
            )
        }
    }
}

// ===== 可复用组件 =====

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showArrow, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标背景
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = iconTint.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThemeSettingItem(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val themeLabel = when (currentTheme) {
        ThemeMode.LIGHT -> "浅色模式"
        ThemeMode.DARK -> "深色模式"
        ThemeMode.SYSTEM -> "跟随系统"
    }

    val themeIcon = when (currentTheme) {
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "深色模式",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = themeLabel,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            listOf(
                ThemeMode.LIGHT to Pair(Icons.Default.LightMode, "浅色模式"),
                ThemeMode.DARK to Pair(Icons.Default.DarkMode, "深色模式"),
                ThemeMode.SYSTEM to Pair(Icons.Default.SettingsBrightness, "跟随系统")
            ).forEach { (mode, pair) ->
                val (icon, label) = pair
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (currentTheme == mode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                            if (currentTheme == mode) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onThemeChange(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
fun BudgetSettingDialog(
    currentBudget: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget?.toString() ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置月度预算") },
        text = {
            Column {
                Text(
                    text = "设置每月的总支出预算，帮助您控制消费",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it
                        showError = false
                    },
                    label = { Text("预算金额") },
                    placeholder = { Text("例如：3000") },
                    leadingIcon = { Text("¥", fontSize = 20.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("请输入有效的金额", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "提示：留空表示不设置预算",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (budgetText.isBlank()) {
                        onConfirm(null)  // 清除预算
                    } else {
                        val budget = budgetText.toDoubleOrNull()
                        if (budget != null && budget > 0) {
                            onConfirm(budget)
                        } else {
                            showError = true
                        }
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())

    val customCategoriesCount = categories.count { !it.isDefault }
    val scope = rememberCoroutineScope()

    var showClearExpensesConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        title = {
            Text("数据管理", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 数据统计
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "数据统计",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DataStatItem(
                                label = "消费记录",
                                value = "${expenses.size}",
                                icon = Icons.Default.Receipt
                            )
                            DataStatItem(
                                label = "快速模板",
                                value = "${templates.size}",
                                icon = Icons.Default.Bookmark
                            )
                            DataStatItem(
                                label = "自定义类别",
                                value = "$customCategoriesCount",
                                icon = Icons.Default.Category
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 危险操作警告
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "危险操作",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "以下操作无法撤销，请谨慎操作",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 清除消费记录按钮
                OutlinedButton(
                    onClick = { showClearExpensesConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除所有消费记录 (${expenses.size})")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 恢复出厂设置按钮
                Button(
                    onClick = { showClearAllConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("恢复出厂设置")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    // 清除消费记录确认对话框
    if (showClearExpensesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearExpensesConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("清除所有消费记录？") },
            text = {
                Text("将删除所有 ${expenses.size} 条消费记录，但保留类别和模板。\n\n此操作无法撤销！")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllExpenses()
                            showClearExpensesConfirm = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确定删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearExpensesConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 恢复出厂设置确认对话框
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("恢复出厂设置？") },
            text = {
                Column {
                    Text("将删除以下所有数据：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${expenses.size} 条消费记录")
                    Text("• ${templates.size} 个快速模板")
                    Text("• $customCategoriesCount 个自定义类别")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "默认类别将保留。\n\n此操作无法撤销！",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllData()
                            showClearAllConfirm = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确定恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DataStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ExpenseAlertDialog(
    currentThreshold: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var thresholdText by remember { mutableStateOf(currentThreshold?.toInt()?.toString() ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = { Text("设置消费提醒") },
        text = {
            Column {
                Text(
                    text = "当单笔消费超过设定金额时，将弹出确认对话框",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it
                        showError = false
                    },
                    label = { Text("提醒金额") },
                    placeholder = { Text("例如：500") },
                    leadingIcon = { Text("¥", fontSize = 20.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("请输入有效的金额", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 快捷选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 200, 500, 1000).forEach { amount ->
                        FilterChip(
                            selected = thresholdText == amount.toString(),
                            onClick = {
                                thresholdText = amount.toString()
                                showError = false
                            },
                            label = { Text("¥$amount") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "💡 提示：留空表示不设置提醒",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (thresholdText.isBlank()) {
                        onConfirm(null)  // 清除提醒
                    } else {
                        val threshold = thresholdText.toDoubleOrNull()
                        if (threshold != null && threshold > 0) {
                            onConfirm(threshold)
                        } else {
                            showError = true
                        }
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.data.ThemePreferences
import com.example.myapplication.data.getCurrencySymbol
import com.example.myapplication.utils.LanguageManager
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    currentFontScale: String,
    onFontScaleChange: (String) -> Unit,
    currentBudget: Double?,
    onBudgetChange: (Double?) -> Unit,
    viewModel: ExpenseViewModel,
    currentAlertThreshold: Double?,
    onAlertThresholdChange: (Double?) -> Unit,
    showPersistentNotification: Boolean,
    onPersistentNotificationChange: (Boolean) -> Unit,
    requireDeleteConfirm: Boolean,
    onRequireDeleteConfirmChange: (Boolean) -> Unit,
    isGuest: Boolean = false,
    onBack: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onBudgetAchievementUnlocked: () -> Unit = {},
    currentLanguage: LanguageManager.AppLanguage = LanguageManager.AppLanguage.FOLLOW_SYSTEM,
    onLanguageChange: (LanguageManager.AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    val themePreferences = remember(context) { ThemePreferences(context) }
    val selectedCurrency by themePreferences.selectedCurrency.collectAsState(initial = "HKD")
    val currencyScope = rememberCoroutineScope()

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        SettingsSectionTitle(title = stringResource(R.string.settings_account))

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.account_title),
                subtitle = stringResource(R.string.settings_account),
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    if (isGuest) {
                        onNavigateToLogin()
                    } else {
                        onNavigateToAccount()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 外观设置 =====
        SettingsSectionTitle(title = stringResource(R.string.settings_appearance))

        SettingsCard {
            // 深色模式
            ThemeSettingItem(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            FontScaleSettingItem(
                currentFontScale = currentFontScale,
                onFontScaleChange = onFontScaleChange
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            CurrencySettingItem(
                currentCurrency = selectedCurrency,
                onCurrencyChange = { currency ->
                    currencyScope.launch {
                        themePreferences.setCurrency(currency)
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            // 语言选择
             LanguageSettingItem(
                 currentLanguage = currentLanguage,
                 onLanguageChange = onLanguageChange
             )
        }

        Spacer(modifier = Modifier.height(16.dp))

// ===== 预算设置 =====
        SettingsSectionTitle(title = stringResource(R.string.settings_budget))

        var showBudgetDialog by remember { mutableStateOf(false) }
        var showAlertDialog by remember { mutableStateOf(false) }

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.AccountBalance,
                title = stringResource(R.string.settings_monthly_budget),
                subtitle = if (currentBudget != null) "¥${"%.2f".format(currentBudget)}" else stringResource(R.string.settings_not_set),
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = { showBudgetDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))  // 添加分隔线

            SettingsItem(  // 添加消费提醒
                icon = Icons.Default.NotificationsActive,
                title = stringResource(R.string.settings_alert_threshold),
                subtitle = if (currentAlertThreshold != null)
                    stringResource(R.string.settings_alert_threshold_value, "¥${"%.0f".format(currentAlertThreshold)}")
                else
                    stringResource(R.string.settings_not_set),
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
                    text = stringResource(R.string.settings_persistent_notification),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.settings_persistent_notification_desc),
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
                    text = stringResource(R.string.settings_delete_confirm),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.settings_delete_confirm_desc),
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
                    if (newBudget != null && newBudget > 0.0) {
                        onBudgetAchievementUnlocked()
                    }
                    showBudgetDialog = false
                }
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

// ===== 数据管理 =====
        SettingsSectionTitle(title = stringResource(R.string.settings_data))

        var showDataManagementDialog by remember { mutableStateOf(false) }

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Storage,
                title = stringResource(R.string.settings_data),
                subtitle = stringResource(R.string.settings_data_desc),
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
        SettingsSectionTitle(title = stringResource(R.string.settings_export_csv))

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.settings_export_csv),
                subtitle = stringResource(R.string.settings_export_csv_desc),
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToExport
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 关于 =====
        SettingsSectionTitle(title = stringResource(R.string.settings_about))

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                showArrow = false
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsItem(
                icon = Icons.Default.Code,
                title = stringResource(R.string.settings_tech_stack),
                subtitle = stringResource(R.string.settings_tech_stack_value),
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
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
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
                    text = stringResource(R.string.settings_theme),
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
                ThemeMode.LIGHT to Pair(Icons.Default.LightMode, stringResource(R.string.settings_theme_light)),
                ThemeMode.DARK to Pair(Icons.Default.DarkMode, stringResource(R.string.settings_theme_dark)),
                ThemeMode.SYSTEM to Pair(Icons.Default.SettingsBrightness, stringResource(R.string.settings_theme_system))
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
        title = { Text(stringResource(R.string.settings_budget_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_budget_dialog_desc),
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
                    label = { Text(stringResource(R.string.settings_monthly_budget)) },
                    placeholder = { Text(stringResource(R.string.settings_budget_placeholder)) },
                    leadingIcon = { Text("¥", fontSize = 20.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.common_enter_valid_amount), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.settings_budget_dialog_hint),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
            Text(stringResource(R.string.settings_data), style = MaterialTheme.typography.titleLarge)
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
                            text = stringResource(R.string.settings_data_stats),
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
                                label = stringResource(R.string.settings_data_stat_expenses),
                                value = "${expenses.size}",
                                icon = Icons.Default.Receipt
                            )
                            DataStatItem(
                                label = stringResource(R.string.settings_data_stat_templates),
                                value = "${templates.size}",
                                icon = Icons.Default.Bookmark
                            )
                            DataStatItem(
                                label = stringResource(R.string.settings_data_stat_custom_categories),
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
                        text = stringResource(R.string.settings_danger_zone),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.settings_data_irreversible_warning),
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
                    Text(stringResource(R.string.settings_clear_expenses_button, expenses.size))
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
                    Text(stringResource(R.string.settings_clear_data))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
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
            title = { Text(stringResource(R.string.settings_clear_expenses_confirm_title)) },
            text = {
                Text(stringResource(R.string.settings_clear_expenses_confirm_message, expenses.size))
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
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearExpensesConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
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
            title = { Text(stringResource(R.string.settings_clear_data_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_clear_data_confirm_intro))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_clear_data_expenses_line, expenses.size))
                    Text(stringResource(R.string.settings_clear_data_templates_line, templates.size))
                    Text(stringResource(R.string.settings_clear_data_categories_line, customCategoriesCount))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_clear_data_confirm_footer),
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
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.settings_alert_threshold)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_alert_threshold_hint),
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
                    label = { Text(stringResource(R.string.settings_alert_amount_label)) },
                    placeholder = { Text(stringResource(R.string.settings_alert_placeholder)) },
                    leadingIcon = { Text("¥", fontSize = 20.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.common_enter_valid_amount), color = MaterialTheme.colorScheme.error) }
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
                    text = stringResource(R.string.settings_alert_hint),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun LanguageSettingItem(
    currentLanguage: LanguageManager.AppLanguage,
    onLanguageChange: (LanguageManager.AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = currentLanguage.displayName,
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

        // 语言选择下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            LanguageManager.AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.displayName,
                                fontWeight = if (currentLanguage == language)
                                    FontWeight.SemiBold else FontWeight.Normal,
                                color = if (currentLanguage == language)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (currentLanguage == language) {
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
                        onLanguageChange(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CurrencySettingItem(
    currentCurrency: String,
    onCurrencyChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val subtitle = "${getCurrencySymbol(currentCurrency)} ($currentCurrency)"
    val options = listOf("HKD", "CNY", "USD")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_currency),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
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

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.settings_currency)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { currency ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCurrencyChange(currency)
                                        showDialog = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentCurrency == currency,
                                    onClick = {
                                        onCurrencyChange(currency)
                                        showDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$currency — ${getCurrencySymbol(currency)}")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            )
        }
    }
}

@Composable
fun FontScaleSettingItem(
    currentFontScale: String,
    onFontScaleChange: (String) -> Unit
) {
    val options = listOf(
        "small" to stringResource(R.string.font_small),
        "medium" to stringResource(R.string.font_medium),
        "large" to stringResource(R.string.font_large)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_font_size),
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = currentFontScale == value,
                    onClick = { onFontScaleChange(value) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

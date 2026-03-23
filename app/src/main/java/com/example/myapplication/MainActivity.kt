package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.data.ThemePreferences
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.data.NotificationHelper
import com.example.myapplication.ui.theme.PurpleStart
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.ui.ExportPage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限已授予，更新通知
            lifecycleScope.launch {
                val prefs = ThemePreferences(this@MainActivity)
                prefs.showPersistentNotification.collect { show ->
                    if (show) {
                        // 触发通知更新
                    }
                }
            }
        }
    }
    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(
            (application as FinanceApplication).repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建通知渠道
        NotificationHelper.createNotificationChannels(this)
        // 请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val themePreferences = ThemePreferences(this)

        setContent {
            // 读取主题设置
            val themeMode by themePreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val monthlyBudget by themePreferences.monthlyBudget
                .collectAsStateWithLifecycle(initialValue = null)

            val expenseAlertThreshold by themePreferences.expenseAlertThreshold
                .collectAsStateWithLifecycle(initialValue = null)

            val showPersistentNotification by themePreferences.showPersistentNotification
                .collectAsStateWithLifecycle(initialValue = false)

            // 根据设置决定是否使用深色模式
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {

                MainScreen(
                    viewModel = viewModel,
                    themePreferences = themePreferences,
                    currentTheme = themeMode,
                    currentBudget = monthlyBudget,
                    currentAlertThreshold = expenseAlertThreshold,
                    showPersistentNotification = showPersistentNotification,
                    context = this,
                    navigationRoute = intent?.getStringExtra("navigate_to"),
                    quickTemplateId = intent?.getIntExtra("quick_template_id", -1)
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    themePreferences: ThemePreferences,
    currentTheme: ThemeMode,
    currentBudget: Double?,
    currentAlertThreshold: Double?,
    showPersistentNotification: Boolean,
    context: Context,
    navigationRoute: String? = null,
    quickTemplateId: Int? = null
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

// 处理从通知跳转
    LaunchedEffect(navigationRoute) {
        navigationRoute?.let { route ->
            when (route) {
                "record" -> navController.navigate(BottomNavItem.Record.route)
            }
        }
    }

// 处理快速模板记账
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val savingGoals by viewModel.savingGoals.collectAsState(initial = emptyList())
    val stocks by viewModel.stocks.collectAsState(initial = emptyList())
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())

    LaunchedEffect(quickTemplateId) {
        if (quickTemplateId != null && quickTemplateId > 0) {
            val template = templates.find { it.id == quickTemplateId }
            template?.let {
                viewModel.createExpenseFromTemplate(it)
            }
        }
    }
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Record,
        BottomNavItem.Debt,
        BottomNavItem.Saving,
        BottomNavItem.Analysis,
        BottomNavItem.Settings
    )

    LaunchedEffect(monthlyTotal, currentBudget, showPersistentNotification) {
        if (showPersistentNotification && currentBudget != null && currentBudget > 0) {
            // 获取置顶模板
            val templates = viewModel.templates.first()
            val pinnedTemplates = templates.filter { it.isPinned }

            // 显示常驻通知（带模板按钮）
            NotificationHelper.showPersistentNotification(
                context,
                monthlyTotal,
                currentBudget,
                pinnedTemplates
            )

            // 检查是否需要发送警告通知
            val percentage = (monthlyTotal / currentBudget * 100)
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastAlertDate = prefs.getString("last_alert_date", "")
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())

            // 每天只提醒一次
            if (lastAlertDate != today) {
                when {
                    percentage >= 100 -> {
                        NotificationHelper.showBudgetAlertNotification(
                            context,
                            monthlyTotal,
                            currentBudget,
                            isOverBudget = true
                        )
                        prefs.edit().putString("last_alert_date", today).apply()
                    }
                    percentage >= 80 -> {
                        NotificationHelper.showBudgetAlertNotification(
                            context,
                            monthlyTotal,
                            currentBudget,
                            isOverBudget = false
                        )
                        prefs.edit().putString("last_alert_date", today).apply()
                    }
                }
            }
        } else {
            // 关闭常驻通知
            NotificationHelper.cancelPersistentNotification(context)
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute in listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Record.route,
                    BottomNavItem.Debt.route,
                    BottomNavItem.Saving.route,
                    BottomNavItem.Analysis.route,
                    BottomNavItem.Settings.route
                )) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurpleStart,
                                selectedTextColor = PurpleStart,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = PurpleStart.copy(alpha = 0.15f)
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    // 弹出到根节点
                                    popUpTo(navController.graph.id) {
                                        inclusive = false
                                    }
                                    // 避免重复导航到同一页面
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomePage(
                    viewModel = viewModel,
                    onNavigateToEdit = { expense ->
                        navController.navigate("edit_expense/${expense.id}")
                    },
                    onNavigateToRecord = {
                        navController.navigate(BottomNavItem.Record.route)
                    },
                    onNavigateToSaving = {
                        navController.navigate(BottomNavItem.Saving.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToStock = {
                        navController.navigate("stock")
                    },
                    monthlyBudget = currentBudget,
                    savingGoals = savingGoals.filter { !it.isCompleted },
                    stocks = stocks
                )
            }

            composable(BottomNavItem.Record.route) {
                RecordPage(
                    viewModel = viewModel,
                    onNavigateToCategory = {
                        navController.navigate("category_management")
                    },
                    alertThreshold = currentAlertThreshold
                )
            }

            composable(BottomNavItem.Analysis.route) {
                AnalysisPage(
                    viewModel = viewModel,
                    monthlyBudget = currentBudget,
                    onNavigateToStock = {
                        navController.navigate("stock")
                    },
                    expenses = expenses,
                    categories = categories
                )
            }

            composable(BottomNavItem.Settings.route) {
                SettingsPage(
                    currentTheme = currentTheme,
                    onThemeChange = { newTheme ->
                        scope.launch {
                            themePreferences.setThemeMode(newTheme)
                        }
                    },
                    currentBudget = currentBudget,
                    onBudgetChange = { newBudget ->
                        scope.launch {
                            themePreferences.setMonthlyBudget(newBudget)
                        }
                    },
                    viewModel = viewModel,
                    currentAlertThreshold = currentAlertThreshold,
                    onAlertThresholdChange = { newThreshold ->
                        scope.launch {
                            themePreferences.setExpenseAlertThreshold(newThreshold)
                        }
                    },
                    showPersistentNotification = showPersistentNotification,
                    onPersistentNotificationChange = { show ->
                        scope.launch {
                            themePreferences.setShowPersistentNotification(show)
                        }
                    },
                    onNavigateToExport = {
                        navController.navigate("export")
                    }
                )
            }

            composable("debt") {
                DebtPage(viewModel = viewModel)
            }

            composable("saving") {
                SavingGoalPage(viewModel = viewModel)
            }

            composable("stock") {
                StockPage(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("category_management") {
                CategoryManagementPage(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("export") {
                ExportPage(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_expense/{expenseId}",
                arguments = listOf(navArgument("expenseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getInt("expenseId")
                val expenses by viewModel.expenses.collectAsState(initial = emptyList())
                val expense = expenses.find { it.id == expenseId }

                if (expense != null) {
                    EditExpensePage(
                        expense = expense,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
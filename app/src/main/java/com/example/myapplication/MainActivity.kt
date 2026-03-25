package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Rect
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
import com.example.myapplication.ui.components.FeatureHighlightOverlay
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
                "record" -> navController.navigate("record")
            }
        }
    }

// 处理快速模板记账
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val savingGoals by viewModel.savingGoals.collectAsState(initial = emptyList())
    val stocks by viewModel.stocks.collectAsState(initial = emptyList())
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val requireDeleteConfirm by themePreferences.requireDeleteConfirm
        .collectAsStateWithLifecycle(initialValue = true)
    val hasSeenOnboarding by themePreferences.hasSeenOnboarding
        .collectAsStateWithLifecycle(initialValue = false)
    var currentTutorialStep by rememberSaveable { mutableIntStateOf(0) }
    var fabRect by remember { mutableStateOf<Rect?>(null) }
    var firstExpenseRect by remember { mutableStateOf<Rect?>(null) }
    var analysisTabRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(hasSeenOnboarding) {
        if (!hasSeenOnboarding) currentTutorialStep = 1
    }

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
        BottomNavItem.Debt,
        BottomNavItem.Saving,
        BottomNavItem.Analysis
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    val showGlobalFab = currentRoute == BottomNavItem.Home.route ||
        currentRoute == BottomNavItem.Analysis.route

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                modifier = if (item.route == BottomNavItem.Analysis.route) {
                                    Modifier.onGloballyPositioned { coordinates ->
                                        analysisTabRect = coordinates.boundsInWindow()
                                    }
                                } else {
                                    Modifier
                                },
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
            },
            floatingActionButton = {
                if (showGlobalFab) {
                    FloatingActionButton(
                        onClick = { navController.navigate("record") },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            fabRect = coordinates.boundsInWindow()
                        },
                        containerColor = PurpleStart,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "记账")
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
                        navController.navigate("record")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToSaving = {
                        navController.navigate(BottomNavItem.Saving.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToStock = {
                        navController.navigate("stock")
                    },
                    onRequestShowTutorial = { currentTutorialStep = 1 },
                    onFirstExpenseBoundsChanged = { rect -> firstExpenseRect = rect },
                    monthlyBudget = currentBudget,
                    requireDeleteConfirm = requireDeleteConfirm,
                    savingGoals = savingGoals.filter { !it.isCompleted },
                    stocks = stocks
                )
            }

            composable("record") {
                RecordPage(
                    viewModel = viewModel,
                    onNavigateToCategory = {
                        navController.navigate("category_management")
                    },
                    onBack = { navController.popBackStack() },
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

            composable("settings") {
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
                    requireDeleteConfirm = requireDeleteConfirm,
                    onRequireDeleteConfirmChange = { req ->
                        scope.launch {
                            themePreferences.setRequireDeleteConfirm(req)
                        }
                    },
                    onBack = { navController.popBackStack() },
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

        if (currentTutorialStep > 0) {
            val stepInfo = when (currentTutorialStep) {
                1 -> if (fabRect != null && fabRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = fabRect,
                        title = "快速记账",
                        description = "点击这里随时随地记录你的开销。这是你管理财务的第一步。",
                        cornerRadius = 50.dp
                    )
                } else null
                2 -> if (firstExpenseRect != null && firstExpenseRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = firstExpenseRect,
                        title = "滑动管理记录",
                        description = "向左滑动可以直接删除账单，向右滑动可以快速修改金额和分类。",
                        cornerRadius = 16.dp
                    )
                } else null
                3 -> if (analysisTabRect != null && analysisTabRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = analysisTabRect,
                        title = "AI 财务管家",
                        description = "在这里查看 AI 为你生成的深度消费报告和存钱建议。",
                        cornerRadius = 50.dp
                    )
                } else null
                else -> null
            }

            stepInfo?.let { info ->
                FeatureHighlightOverlay(
                    targetRect = info.rect,
                    title = info.title,
                    description = info.description,
                    cornerRadius = info.cornerRadius,
                    isLastStep = currentTutorialStep == 3,
                    onNext = {
                        when (currentTutorialStep) {
                            1 -> currentTutorialStep = if (firstExpenseRect != null) 2 else 3
                            2 -> currentTutorialStep = 3
                            else -> {
                                currentTutorialStep = 0
                                scope.launch { themePreferences.setHasSeenOnboarding(true) }
                            }
                        }
                    },
                    onSkip = {
                        currentTutorialStep = 0
                        scope.launch { themePreferences.setHasSeenOnboarding(true) }
                    }
                )
            }
        }
    }
}

private data class TutorialStepInfo(
    val rect: Rect?,
    val title: String,
    val description: String,
    val cornerRadius: androidx.compose.ui.unit.Dp
)

package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.myapplication.data.ThemeMode
import com.example.myapplication.data.ThemePreferences
import com.example.myapplication.data.getCurrencySymbol
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.data.NotificationHelper
import com.example.myapplication.ui.theme.PurpleStart
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.ui.ExportPage
import com.example.myapplication.ui.IncomePage
import com.example.myapplication.ui.AccountPage
import com.example.myapplication.ui.AiReportHistoryPage
import com.example.myapplication.ui.CheckInPage
import com.example.myapplication.ui.CheckInViewModel
import com.example.myapplication.ui.components.FeatureHighlightOverlay
import com.example.myapplication.ui.SyncViewModel
import com.example.myapplication.ui.SyncState
import com.example.myapplication.utils.LanguageManager
import com.example.myapplication.ui.LanguageSelectionPage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MainActivity : AppCompatActivity() {

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
            (application as FinanceApplication).repository,
            ThemePreferences(applicationContext),
            (application as FinanceApplication).database.monthlyIncomeDao(),
            (application as FinanceApplication).database.aiReportDao(),
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply persisted locale before Activity creation so Compose starts in the right language.
        LanguageManager.restoreLanguageOnStartup(this)
        super.onCreate(savedInstanceState)

        // Treat launch extras as one-shot actions; avoid replay after Activity recreation.
        val launchNavigationRoute = if (savedInstanceState == null) {
            intent?.getStringExtra("navigate_to")
        } else {
            null
        }
        val launchQuickTemplateId = if (savedInstanceState == null) {
            intent?.getIntExtra("quick_template_id", -1)?.takeIf { it > 0 }
        } else {
            null
        }
        if (savedInstanceState == null) {
            intent?.removeExtra("navigate_to")
            intent?.removeExtra("quick_template_id")
        }

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
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            var showAccountPage by remember { mutableStateOf(false) }
            var showCheckInPage by remember { mutableStateOf(false) }
            var showAuthModal by remember { mutableStateOf(false) }
            val checkInViewModel: CheckInViewModel = viewModel()
            val syncViewModel: SyncViewModel = viewModel(
                factory = SyncViewModel.Factory(
                    (application as FinanceApplication).syncRepository
                )
            )
            val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()

            val googleSignInClient = remember {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(this, gso)
            }

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (!idToken.isNullOrBlank()) {
                        authViewModel.signInWithGoogle(idToken)
                    }
                } catch (_: ApiException) {
                    // Keep user on auth screen; AuthPage handles explicit Firebase errors.
                }
            }

            // 读取主题设置
            val themeMode by themePreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val currentLanguage by LanguageManager.languageFlow(this@MainActivity)
                .collectAsStateWithLifecycle(initialValue = LanguageManager.AppLanguage.FOLLOW_SYSTEM)

            val hasChosenLanguage by LanguageManager.hasChosenLanguageFlow(this@MainActivity)
                .collectAsStateWithLifecycle(initialValue = true)  // true = 不显示选择页，避免闪屏

            val monthlyBudget by themePreferences.monthlyBudget
                .collectAsStateWithLifecycle(initialValue = null)

            val expenseAlertThreshold by themePreferences.expenseAlertThreshold
                .collectAsStateWithLifecycle(initialValue = null)

            val showPersistentNotification by themePreferences.showPersistentNotification
                .collectAsStateWithLifecycle(initialValue = false)

            val autoSyncEnabled by themePreferences.autoSyncEnabled
                .collectAsStateWithLifecycle(initialValue = false)

            val fontScaleSetting by themePreferences.fontScaleFlow
                .collectAsStateWithLifecycle(initialValue = "medium")

            val fontScaleFloat = when (fontScaleSetting) {
                "small" -> 0.85f
                "large" -> 1.15f
                else -> 1.0f
            }

            LaunchedEffect(authState) {
                if (authState !is AuthState.Authenticated) {
                    showAccountPage = false
                }
                if (authState !is AuthState.Guest) {
                    showAuthModal = false
                }
            }

            // 自动同步：登录状态变化为已登录时触发一次
            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    syncViewModel.onFirstSyncCompleted = {
                        checkInViewModel.unlockAchievement("first_sync")
                    }
                    syncViewModel.syncNow()
                }
            }

            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    val budget = themePreferences.monthlyBudget.first() ?: 0.0
                    if (budget > 0.0) {
                        val allExpenses = viewModel.expenses.first()
                        val monthlyTotals = allExpenses
                            .filter { !it.isDeleted }
                            .groupBy {
                                java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                                    .format(java.util.Date(it.date))
                            }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }
                        checkInViewModel.checkBudgetAchievementsOnStartup(monthlyTotals, budget)
                    }
                }
            }

            // 根据设置决定是否使用深色模式
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = currentDensity.density,
                        fontScale = fontScaleFloat
                    )
                ) {
                    // 首次启动：显示语言选择页，选完后再进入正常流程
                    if (!hasChosenLanguage) {
                        LanguageSelectionPage(
                            onLanguageConfirmed = { language ->
                                lifecycleScope.launch {
                                    LanguageManager.markLanguageChosen(this@MainActivity)
                                    LanguageManager.saveLanguage(this@MainActivity, language)
                                    // saveLanguage 内部会调用 AppCompatDelegate.setApplicationLocales()
                                    // 这会触发 Activity 重建，语言选择页自动消失
                                }
                            }
                        )
                        return@CompositionLocalProvider
                    }
                    when (authState) {
                    AuthState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is AuthState.Authenticated -> {
                        if (showCheckInPage) {
                            CheckInPage(
                                viewModel = checkInViewModel,
                                onBack = {
                                    showCheckInPage = false
                                    showAccountPage = true
                                }
                            )
                        } else if (showAccountPage) {
                            AccountPage(
                                authViewModel = authViewModel,
                                autoSyncEnabled = autoSyncEnabled,
                                onAutoSyncToggle = { enabled: Boolean ->
                                    lifecycleScope.launch {
                                        themePreferences.setAutoSyncEnabled(enabled)
                                    }
                                },
                                onSyncNow = {
                                    syncViewModel.onFirstSyncCompleted = {
                                        checkInViewModel.unlockAchievement("first_sync")
                                    }
                                    syncViewModel.syncNow()
                                },
                                isSyncing = syncState is SyncState.Syncing,
                                syncMessage = when (val s = syncState) {
                                    is SyncState.Success -> stringResource(
                                        R.string.sync_result_uploaded_downloaded,
                                        s.uploaded,
                                        s.downloaded
                                    )
                                    is SyncState.Error -> stringResource(
                                        R.string.account_sync_failed_with_reason,
                                        s.message
                                    )
                                    else -> ""
                                },
                                onNavigateToCheckIn = {
                                    showAccountPage = false
                                    showCheckInPage = true
                                },
                                onNavigateBack = { showAccountPage = false }
                            )
                        } else {
                            MainScreen(
                                viewModel = viewModel,
                                authViewModel = authViewModel,
                                checkInViewModel = checkInViewModel,
                                themePreferences = themePreferences,
                                currentTheme = themeMode,
                                currentFontScale = fontScaleSetting,
                                onFontScaleChange = { scale ->
                                    lifecycleScope.launch {
                                        themePreferences.saveFontScale(scale)
                                    }
                                },
                                currentLanguage = currentLanguage,
                                onLanguageChange = { newLanguage ->
                                    lifecycleScope.launch {
                                        LanguageManager.saveLanguage(this@MainActivity, newLanguage)
                                    }
                                },
                                currentBudget = monthlyBudget,
                                currentAlertThreshold = expenseAlertThreshold,
                                showPersistentNotification = showPersistentNotification,
                                context = this@MainActivity,
                                navigationRoute = launchNavigationRoute,
                                quickTemplateId = launchQuickTemplateId,
                                isGuest = false,
                                onNavigateToAccount = { showAccountPage = true },
                                onNavigateToLogin = {}
                            )
                        }
                    }

                    AuthState.Guest -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MainScreen(
                                viewModel = viewModel,
                                authViewModel = authViewModel,
                                checkInViewModel = checkInViewModel,
                                themePreferences = themePreferences,
                                currentTheme = themeMode,
                                currentFontScale = fontScaleSetting,
                                onFontScaleChange = { scale ->
                                    lifecycleScope.launch {
                                        themePreferences.saveFontScale(scale)
                                    }
                                },
                                currentLanguage = currentLanguage,
                                onLanguageChange = { newLanguage ->
                                    lifecycleScope.launch {
                                        LanguageManager.saveLanguage(this@MainActivity, newLanguage)
                                    }
                                },
                                currentBudget = monthlyBudget,
                                currentAlertThreshold = expenseAlertThreshold,
                                showPersistentNotification = showPersistentNotification,
                                context = this@MainActivity,
                                navigationRoute = launchNavigationRoute,
                                quickTemplateId = launchQuickTemplateId,
                                isGuest = true,
                                onNavigateToAccount = {},
                                onNavigateToLogin = { showAuthModal = true }
                            )

                            if (showAuthModal) {
                                Dialog(
                                    onDismissRequest = { showAuthModal = false },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Surface(modifier = Modifier.fillMaxSize()) {
                                        AuthPage(
                                            authViewModel = authViewModel,
                                            onLoginSuccess = {},
                                            onGoogleSignInClick = {
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                            },
                                            onContinueAsGuest = {
                                                showAuthModal = false
                                                authViewModel.enterGuestMode()
                                            },
                                            onDismiss = { showAuthModal = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is AuthState.EmailNotVerified -> {
                        EmailVerificationPage(authViewModel = authViewModel)
                    }

                    AuthState.Unauthenticated,
                    is AuthState.Error -> {
                        AuthPage(
                            authViewModel = authViewModel,
                            onLoginSuccess = {},
                            onGoogleSignInClick = {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            onContinueAsGuest = { authViewModel.enterGuestMode() },
                            onDismiss = null
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    authViewModel: AuthViewModel,
    themePreferences: ThemePreferences,
    currentTheme: ThemeMode,
    currentFontScale: String,
    onFontScaleChange: (String) -> Unit,
    currentLanguage: LanguageManager.AppLanguage,
    onLanguageChange: (LanguageManager.AppLanguage) -> Unit,
    currentBudget: Double?,
    currentAlertThreshold: Double?,
    showPersistentNotification: Boolean,
    context: Context,
    navigationRoute: String? = null,
    quickTemplateId: Int? = null,
    isGuest: Boolean = false,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    checkInViewModel: CheckInViewModel
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
        .map { it as Boolean? }
        .collectAsStateWithLifecycle(initialValue = null)
    var currentTutorialStep by rememberSaveable { mutableIntStateOf(0) }
    var fabRect by remember { mutableStateOf<Rect?>(null) }
    var firstExpenseRect by remember { mutableStateOf<Rect?>(null) }
    var analysisTabRect by remember { mutableStateOf<Rect?>(null) }
    var galleryCardRect by remember { mutableStateOf<Rect?>(null) }
    var aiInputRect by remember { mutableStateOf<Rect?>(null) }
    var settingsIconRect by remember { mutableStateOf<Rect?>(null) }
    var avatarRect by remember { mutableStateOf<Rect?>(null) }
    var homeScrollState by remember { mutableStateOf<ScrollState?>(null) }

    // Temporary debug logs for tutorial target rects.
    LaunchedEffect(galleryCardRect, aiInputRect, settingsIconRect, avatarRect) {
        android.util.Log.d("TutorialRect", "gallery=$galleryCardRect")
        android.util.Log.d("TutorialRect", "aiInput=$aiInputRect")
        android.util.Log.d("TutorialRect", "settings=$settingsIconRect")
        android.util.Log.d("TutorialRect", "avatar=$avatarRect")
    }

    LaunchedEffect(hasSeenOnboarding) {
        if (hasSeenOnboarding == false) currentTutorialStep = 1
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
            val currencySymbol = getCurrencySymbol(themePreferences.selectedCurrency.first())
            // 获取置顶模板
            val templates = viewModel.templates.first()
            val pinnedTemplates = templates.filter { it.isPinned }

            // 显示常驻通知（带模板按钮）
            NotificationHelper.showPersistentNotification(
                context,
                monthlyTotal,
                currentBudget,
                currencySymbol,
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
                            currencySymbol,
                            isOverBudget = true
                        )
                        prefs.edit().putString("last_alert_date", today).apply()
                    }
                    percentage >= 80 -> {
                        NotificationHelper.showBudgetAlertNotification(
                            context,
                            monthlyTotal,
                            currentBudget,
                            currencySymbol,
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
                                icon = { Icon(item.icon, contentDescription = stringResource(item.titleRes)) },
                                label = { Text(stringResource(item.titleRes)) },
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
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.record_title))
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(
                    bottom = innerPadding.calculateBottomPadding()
                )
            ) {
            composable(BottomNavItem.Home.route) {
                HomePage(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onNavigateToEdit = { expense ->
                        navController.navigate("edit_expense/${expense.id}")
                    },
                    onNavigateToRecord = {
                        navController.navigate("record")
                    },
                    onNavigateToAccount = {
                        if (isGuest) {
                            onNavigateToLogin()
                        } else {
                            onNavigateToAccount()
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToIncome = {
                        navController.navigate("income")
                    },
                    onNavigateToSaving = {
                        navController.navigate(BottomNavItem.Saving.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToStock = {
                        navController.navigate("stock")
                    },
                    onRequestShowTutorial = {
                        scope.launch {
                            // Scroll to top first so scroll-container targets have stable positions.
                            homeScrollState?.scrollTo(0)
                            // Wait for scroll + relayout before starting onboarding.
                            delay(500)
                            currentTutorialStep = 1
                        }
                    },
                    onScrollStateReady = { homeScrollState = it },
                    onFirstExpenseBoundsChanged = { rect -> firstExpenseRect = rect },
                    onGalleryCardBoundsChanged = { rect -> galleryCardRect = rect },
                    onAiInputBoundsChanged = { rect -> aiInputRect = rect },
                    onSettingsIconBoundsChanged = { rect -> settingsIconRect = rect },
                    onAvatarBoundsChanged = { rect -> avatarRect = rect },
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
                        onAchievementUnlocked = { id: String ->
                            checkInViewModel.unlockAchievement(id)
                        },
                        checkInViewModel = checkInViewModel,
                        onBack = { navController.popBackStack() },
                        alertThreshold = currentAlertThreshold,
                        isGuest = isGuest,
                        onNavigateToLogin = onNavigateToLogin
                    )
                }

                composable(BottomNavItem.Analysis.route) {
                    AnalysisPage(
                        viewModel = viewModel,
                        monthlyBudget = currentBudget,
                        onNavigateToStock = {
                            navController.navigate("stock")
                        },
                        onNavigateToSaving = {
                            navController.navigate(BottomNavItem.Saving.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        onNavigateToCheckIn = onNavigateToAccount,
                        onNavigateToReportHistory = { navController.navigate("ai_report_history") },
                        expenses = expenses,
                        categories = categories,
                        isGuest = isGuest,
                        onNavigateToLogin = onNavigateToLogin,
                        onFirstAnalyzeGenerated = {
                            checkInViewModel.unlockAchievement("first_ai_analyze")
                        },
                        checkInViewModel = checkInViewModel,
                    )
                }

            composable("settings") {
                SettingsPage(
                    currentTheme = currentTheme,
                    currentFontScale = currentFontScale,
                    onFontScaleChange = onFontScaleChange,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
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
                    isGuest = isGuest,
                    onBack = { navController.popBackStack() },
                    onNavigateToExport = {
                        navController.navigate("export")
                    },
                    onNavigateToAccount = onNavigateToAccount,
                    onNavigateToLogin = onNavigateToLogin,
                    onBudgetAchievementUnlocked = {
                        checkInViewModel.unlockAchievement("set_budget")
                    },
                )
            }

            composable("debt") {
                DebtPage(
                    viewModel = viewModel,
                    onFirstLoanCreated = {
                        checkInViewModel.unlockAchievement("first_loan")
                    }
                )
            }

            composable("income") {
                IncomePage(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onFirstIncomeRecorded = {
                        checkInViewModel.unlockAchievement("first_income")
                    }
                )
            }

            composable("saving") {
                SavingGoalPage(
                    viewModel = viewModel,
                    onFirstGoalCreated = {
                        checkInViewModel.unlockAchievement("set_saving_goal")
                    }
                )
            }

            composable("stock") {
                StockPage(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onFirstStockAdded = {
                        checkInViewModel.unlockAchievement("first_stock")
                    }
                )
            }

            composable("ai_report_history") {
                AiReportHistoryPage(
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
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            val stepInfo = when (currentTutorialStep) {
                1 -> if (fabRect != null && fabRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = fabRect,
                        title = stringResource(R.string.onboarding_step1_title),
                        description = stringResource(R.string.onboarding_step1_desc),
                        cornerRadius = 50.dp
                    )
                } else null
                2 -> TutorialStepInfo(
                    rect = galleryCardRect?.takeIf {
                        it.width > 0f && it.top > 0f && it.top < screenHeightPx * 0.8f
                    } ?: Rect(
                        left = 0f,
                        top = screenHeightPx * 0.25f,
                        right = screenWidthPx,
                        bottom = screenHeightPx * 0.55f
                    ),
                    title = stringResource(R.string.onboarding_step2_title),
                    description = stringResource(R.string.onboarding_step2_desc),
                    cornerRadius = 20.dp
                )
                3 -> TutorialStepInfo(
                    rect = null,
                    title = stringResource(R.string.onboarding_step3_title),
                    description = stringResource(R.string.onboarding_step3_desc),
                    cornerRadius = 8.dp
                )
                4 -> aiInputRect
                    ?.takeIf { it.width > 0f && it.top < 2000f }
                    ?.let {
                    TutorialStepInfo(
                        rect = it,
                        title = stringResource(R.string.onboarding_step4_title),
                        description = stringResource(R.string.onboarding_step4_desc),
                        cornerRadius = 16.dp
                    )
                }
                5 -> if (analysisTabRect != null && analysisTabRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = analysisTabRect,
                        title = stringResource(R.string.onboarding_step5_title),
                        description = stringResource(R.string.onboarding_step5_desc),
                        cornerRadius = 50.dp
                    )
                } else null
                6 -> if (settingsIconRect != null && settingsIconRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = settingsIconRect,
                        title = stringResource(R.string.onboarding_step6_title),
                        description = stringResource(R.string.onboarding_step6_desc),
                        cornerRadius = 50.dp
                    )
                } else null
                7 -> if (avatarRect != null && avatarRect!!.width > 0f) {
                    TutorialStepInfo(
                        rect = avatarRect,
                        title = stringResource(R.string.onboarding_step7_title),
                        description = stringResource(R.string.onboarding_step7_desc),
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
                    isLastStep = currentTutorialStep == 7,
                    onNext = {
                        when (currentTutorialStep) {
                            1 -> currentTutorialStep = if (galleryCardRect != null) 2 else 3
                            2 -> currentTutorialStep = if (firstExpenseRect != null) 3 else 4
                            3 -> currentTutorialStep = 4
                            4 -> currentTutorialStep = 5
                            5 -> currentTutorialStep = 6
                            6 -> currentTutorialStep = 7
                            7 -> {
                                currentTutorialStep = 0
                                scope.launch { themePreferences.setHasSeenOnboarding(true) }
                            }
                            else -> Unit
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

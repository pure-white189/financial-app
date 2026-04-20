package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.data.MonthlyIncome
import com.example.myapplication.data.SavingGoal
import com.example.myapplication.data.Stock
import com.example.myapplication.data.getCurrencySymbol
import com.example.myapplication.ui.components.RecommendationCard
import com.example.myapplication.utils.displayName
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "→",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(viewModel: ExpenseViewModel,
             authViewModel: AuthViewModel,
             onNavigateToEdit: (Expense) -> Unit = {},
             onNavigateToRecord: () -> Unit = {},
             onNavigateToAccount: () -> Unit = {},
             onNavigateToSettings: () -> Unit = {},
             onNavigateToIncome: () -> Unit = {},
             onNavigateToSaving: () -> Unit = {},
             onNavigateToStock: () -> Unit = {},
             onRequestShowTutorial: () -> Unit = {},
             onScrollStateReady: (ScrollState) -> Unit = {},
             onFirstExpenseBoundsChanged: (Rect?) -> Unit = {},
             onGalleryCardBoundsChanged: (Rect?) -> Unit = {},
             onAiInputBoundsChanged: (Rect?) -> Unit = {},
             onSettingsIconBoundsChanged: (Rect?) -> Unit = {},
             onAvatarBoundsChanged: (Rect?) -> Unit = {},
             monthlyBudget: Double? = null,
             requireDeleteConfirm: Boolean = true,
             savingGoals: List<SavingGoal> = emptyList(),
             stocks: List<Stock> = emptyList()
) {
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val allIncome by viewModel.allIncome.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val mainCurrencyCode by viewModel.mainCurrencyCode.collectAsState()
    val recommendationsJson by viewModel.recommendationsJson.collectAsState()
    val todayRecommendation by viewModel.todayRecommendation.collectAsState()
    val insightDismissedDate by viewModel.insightDismissedDate.collectAsState()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val homeScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFilter by remember { mutableStateOf(TimeFilter.ALL) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var hasSeenSwipeHint by remember { mutableStateOf(prefs.getBoolean("has_seen_swipe_hint", false)) }
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authViewModel.currentUser
    val avatarInitial = (currentUser?.displayName?.firstOrNull()
        ?: currentUser?.email?.firstOrNull()
        ?: '?').toString().uppercase()
    val currentYearMonth = remember {
        val calendar = Calendar.getInstance()
        "%d-%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
    val currentMonthIncome = remember(allIncome, currentYearMonth) {
        allIncome.firstOrNull { it.yearMonth == currentYearMonth }
    }
    val currentMonthBalance = currentMonthIncome?.amount?.minus(monthlyTotal)

    LaunchedEffect(homeScrollState) {
        onScrollStateReady(homeScrollState)
    }

    // 根据筛选条件过滤消费记录
    val filteredExpenses = remember(expenses, selectedFilter) {
        when (selectedFilter) {
            TimeFilter.TODAY -> {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= todayStart }
            }
            TimeFilter.WEEK -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= weekStart }
            }
            TimeFilter.MONTH -> {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= monthStart }
            }
            else -> expenses
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(homeScrollState)
                .padding(16.dp)
        ) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = {
                Text(
                    text = stringResource(R.string.home_overview_title),
                    fontSize = 28.sp,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateToAccount,
                    modifier = Modifier
                        .size(36.dp)
                        .onGloballyPositioned { coordinates ->
                            onAvatarBoundsChanged(coordinates.boundsInWindow())
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarInitial,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            },
            actions = {
                FilledTonalIconButton(
                    onClick = onRequestShowTutorial,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.home_tutorial_help),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        onSettingsIconBoundsChanged(coordinates.boundsInWindow())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.common_settings),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        val todayTotal = remember(expenses) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            expenses.filter { it.date >= todayStart }
                .sumOf { it.amount }
        }

        val weeklyTotal = remember(expenses) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfWeek = calendar.timeInMillis

            expenses.filter { it.date >= startOfWeek }
                .sumOf { it.amount }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onAiInputBoundsChanged(coordinates.boundsInWindow())
                }
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PurpleStart, PurpleEnd)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    stringResource(R.string.home_monthly_spent_title),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$currencySymbol %.2f".format(monthlyTotal),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroStatItem(stringResource(R.string.home_today), "$currencySymbol%.0f".format(todayTotal))
                    HeroStatItem(stringResource(R.string.home_this_week), "$currencySymbol%.0f".format(weeklyTotal))
                }
            }
        }

        val ongoingSavingGoals = remember(savingGoals) {
            savingGoals.filter { !it.isCompleted }
        }

        // 判断是否有数据需要展示在概览区
        val hasBudget = monthlyBudget != null && monthlyBudget > 0
        val hasGoals = ongoingSavingGoals.isNotEmpty()
        val hasStocks = stocks.isNotEmpty()
        val showIncomeCard = true

        if (showIncomeCard || hasBudget || hasGoals || hasStocks) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.home_assets_budget_title),
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        onGalleryCardBoundsChanged(coordinates.boundsInWindow())
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    IncomeSummaryCard(
                        currentMonthIncome = currentMonthIncome,
                        monthlyExpense = monthlyTotal,
                        balance = currentMonthBalance,
                        currencySymbol = currencySymbol,
                        onClick = onNavigateToIncome,
                        modifier = Modifier
                            .width(320.dp)
                            .height(176.dp)
                    )
                }
                item {
                    if (hasBudget) {
                        BudgetProgressCard(
                            monthlyTotal = monthlyTotal,
                            monthlyBudget = monthlyBudget,
                            currencySymbol = currencySymbol,
                            modifier = Modifier
                                .width(320.dp)
                                .height(176.dp)
                        )
                    } else {
                        EmptyStateCard(
                            icon = Icons.Default.PieChart,
                            title = stringResource(R.string.home_empty_budget_title),
                            subtitle = stringResource(R.string.home_empty_budget_subtitle),
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .width(220.dp)
                                .height(176.dp)
                        )
                    }
                }
                if (hasGoals) {
                    item {
                        SavingGoalSummaryCard(
                            ongoingGoals = ongoingSavingGoals,
                            currencySymbol = currencySymbol,
                            onClick = onNavigateToSaving,
                            modifier = Modifier
                                .width(300.dp)
                                .height(176.dp)
                        )
                    }
                }
                item {
                    if (hasStocks) {
                        StockOverviewCard(
                            stocks = stocks,
                            currencySymbol = currencySymbol,
                            onClick = onNavigateToStock,
                            modifier = Modifier
                                .width(300.dp)
                                .height(176.dp)
                        )
                    } else {
                        EmptyStateCard(
                            icon = Icons.Default.ShowChart,
                            title = stringResource(R.string.home_empty_stocks_title),
                            subtitle = stringResource(R.string.home_empty_stocks_subtitle),
                            onClick = onNavigateToStock,
                            modifier = Modifier
                                .width(220.dp)
                                .height(176.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val showInsight = recommendationsJson != null &&
            todayRecommendation != null &&
            insightDismissedDate != today

        AnimatedVisibility(
            visible = showInsight,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.insight_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { viewModel.dismissTodayInsight() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RecommendationCard(
                        recommendationsJson = recommendationsJson!!,
                        trigger = todayRecommendation!!.trigger,
                        stat = todayRecommendation!!.stat,
                        lang = stringResource(R.string.ai_prompt_language),
                        onMapsSearch = { query ->
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                            )
                            context.startActivity(intent)
                        },
                        onNavigateToCheckIn = onNavigateToAccount,
                        onNavigateToSavings = onNavigateToSaving,
                        onNavigateToStocks = onNavigateToStock,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ===== 筛选按钮 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = stringResource(filter.labelRes),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (selected) {
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(50)
                                    )
                            } else {
                                Modifier
                            }
                        ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent,
                        selectedLabelColor = Color.White
                    ),
                    border = null,
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 消费记录标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_recent_expenses),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.home_expense_count, filteredExpenses.size),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 消费记录列表
        if (expenses.isEmpty()) {
            LaunchedEffect(Unit) {
                onFirstExpenseBoundsChanged(null)
            }
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.home_no_expenses),
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_empty_state_desc),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FilledTonalButton(
                        onClick = { onNavigateToRecord() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.home_add_first_expense_cta))
                    }
                }
            }
        } else {
            if (!hasSeenSwipeHint && filteredExpenses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Swipe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.home_swipe_hint_banner),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            hasSeenSwipeHint = true
                            prefs.edit().putBoolean("has_seen_swipe_hint", true).apply()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 消费列表
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    filteredExpenses.forEach { expense ->
                        val isFirst = expense == filteredExpenses.firstOrNull()
                        val category = categories.find { it.id == expense.categoryId }
                        Box(
                            modifier = if (isFirst) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    onFirstExpenseBoundsChanged(coordinates.boundsInWindow())
                                }
                            } else {
                                Modifier
                            }
                        ) {
                            ExpenseItem(
                                expense = expense,
                                category = category,
                                currencySymbol = currencySymbol,
                                mainCurrencyCode = mainCurrencyCode,
                                requireDeleteConfirm = requireDeleteConfirm,
                                onDelete = {
                                    scope.launch {
                                        viewModel.deleteExpense(expense)

                                        if (!requireDeleteConfirm) {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.home_deleted_expense),
                                                actionLabel = context.getString(R.string.common_undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.addExpense(expense)
                                            }
                                        }
                                    }
                                },
                                onEdit = {
                                    onNavigateToEdit(expense)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeSummaryCard(
    currentMonthIncome: MonthlyIncome?,
    monthlyExpense: Double,
    balance: Double?,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceColor = when {
        balance == null -> MaterialTheme.colorScheme.onSurfaceVariant
        balance >= 0 -> IncomeGreen
        else -> ExpenseRed
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.income_this_month),
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = currentMonthIncome?.let {
                        String.format(Locale.getDefault(), "%s %.2f", currencySymbol, it.amount)
                    } ?: "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.income_balance),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = balance?.let {
                        String.format(Locale.getDefault(), "%s %.2f", currencySymbol, it)
                    } ?: "—",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    R.string.income_expense_summary,
                    String.format(Locale.getDefault(), "%s %.2f", currencySymbol, monthlyExpense)
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StockOverviewCard(
    stocks: List<Stock>,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalValue = remember(stocks) { stocks.sumOf { it.currentPrice * it.shares } }
    val totalProfit = remember(stocks) { stocks.sumOf { (it.currentPrice - it.costPrice) * it.shares } }
    val profitColor = if (totalProfit >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_stock_card_title),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.home_stock_count, stocks.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.stock_total_value) + " $currencySymbol${String.format(Locale.getDefault(), "%.2f", totalValue)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.stock_total_profit) + " ${formatSignedAmount(totalProfit, currencySymbol)}",
                fontSize = 14.sp,
                color = profitColor
            )
        }
    }
}

private fun formatSignedAmount(amount: Double, currencySymbol: String): String {
    val sign = if (amount >= 0) "+" else "-"
    return "${sign}${currencySymbol}${String.format(Locale.getDefault(), "%.2f", kotlin.math.abs(amount))}"
}

@Composable
private fun SavingGoalSummaryCard(
    ongoingGoals: List<SavingGoal>,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestGoal = remember(ongoingGoals) { ongoingGoals.maxByOrNull { it.createdAt } } ?: return
    val progress = (latestGoal.currentAmount / latestGoal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val percentage = (progress * 100).toInt()

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_saving_card_title),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.home_saving_goal_count, ongoingGoals.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = latestGoal.name,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%s / %s",
                    String.format(Locale.getDefault(), "$currencySymbol %.2f", latestGoal.currentAmount),
                    String.format(Locale.getDefault(), "$currencySymbol %.2f", latestGoal.targetAmount)
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$percentage%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            if (ongoingGoals.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_saving_goal_more_count, ongoingGoals.size - 1),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    category: Category?,
    currencySymbol: String,
    mainCurrencyCode: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    requireDeleteConfirm: Boolean
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (requireDeleteConfirm) {
                        showDeleteDialog = true
                    } else {
                        onDelete()
                    }
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.debt_delete_confirm)) },
            text = { Text(stringResource(R.string.home_delete_expense_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2196F3)
                SwipeToDismissBoxValue.EndToStart -> Color.Red
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.CenterStart
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                SwipeToDismissBoxValue.Settled -> Icons.Default.Delete
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.8f else 1.2f,
                    label = "icon_scale"
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.scale(scale)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (category?.iconPath != null) {
                        AsyncImage(
                            model = category.iconPath,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Icon(
                            imageVector = getCategoryIcon(category?.iconName),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = category?.displayName() ?: stringResource(R.string.home_unknown_category),
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = DateUtils.formatDate(LocalContext.current, expense.date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (expense.note.isNotEmpty()) {
                            Text(
                                text = expense.note,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val hasOriginalCurrency = expense.originalCurrency != null &&
                    expense.originalCurrency != mainCurrencyCode &&
                    expense.originalAmount != null
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${String.format("%.2f", expense.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasOriginalCurrency) {
                        Text(
                            text = "(${getCurrencySymbol(expense.originalCurrency!!)}${String.format("%.2f", expense.originalAmount!!)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStatItem(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
// 根据图标名称获取图标
fun getCategoryIcon(iconName: String?): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_cart" -> Icons.Default.ShoppingCart
        "movie" -> Icons.Default.Movie
        "local_hospital" -> Icons.Default.LocalHospital
        "school" -> Icons.Default.School
        "home" -> Icons.Default.Home
        else -> Icons.Default.MoreHoriz
    }
}

@Composable
fun BudgetProgressCard(
    monthlyTotal: Double,
    monthlyBudget: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val percentage = (monthlyTotal / monthlyBudget * 100).coerceIn(0.0, 100.0)
    val remaining = monthlyBudget - monthlyTotal

    // 计算距离月底还有多少天
    val calendar = java.util.Calendar.getInstance()
    val today = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val remainingDays = daysInMonth - today + 1

    // 每日可用金额
    val dailyAvailable = if (remaining > 0 && remainingDays > 0) {
        remaining / remainingDays
    } else 0.0

    // 根据进度决定颜色
    val progressColor = when {
        percentage >= 100 -> MaterialTheme.colorScheme.error
        percentage >= 80 -> Color(0xFFFF9800)  // 橙色
        else -> MaterialTheme.colorScheme.primary
    }

    val containerColor = when {
        percentage >= 100 -> MaterialTheme.colorScheme.errorContainer
        percentage >= 80 -> Color(0xFFFF9800).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_budget_progress_title),
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${percentage.toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { (percentage / 100).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 详细信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (remaining >= 0) stringResource(R.string.home_budget_left_short) else stringResource(R.string.home_budget_over_short),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${"%.2f".format(kotlin.math.abs(remaining))}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remaining >= 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.error
                    )
                }

                if (remaining > 0 && remainingDays > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.home_daily_available),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${"%.2f".format(dailyAvailable)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.home_days_to_month_end, remainingDays),
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private enum class TimeFilter(val labelRes: Int) {
    TODAY(R.string.home_today),
    WEEK(R.string.home_this_week),
    MONTH(R.string.home_this_month),
    ALL(R.string.home_filter_all)
}
// 格式化日期
// 格式化日期
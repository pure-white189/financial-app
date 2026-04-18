package com.example.myapplication

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.AiExpenseParser
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import com.example.myapplication.ui.theme.WarningOrange
import com.example.myapplication.utils.displayName
import com.example.myapplication.utils.displayNameNonComposable
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisPage(
    viewModel: ExpenseViewModel,
    monthlyBudget: Double? = null,
    onNavigateToStock: () -> Unit = {},
    expenses: List<Expense> = emptyList(),
    categories: List<Category> = emptyList(),
    isGuest: Boolean = false,
    onNavigateToLogin: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val aiLang = context.getString(R.string.ai_prompt_language)
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val allIncome by viewModel.allIncome.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val scope = rememberCoroutineScope()
    val thisMonthLabel = stringResource(R.string.home_this_month)
    val categoryOtherLabel = stringResource(R.string.category_other)
    val aiFeatureLockedLabel = stringResource(R.string.ai_feature_locked)
    val aiAnalysisErrorLabel = stringResource(R.string.analysis_ai_error)
    var aiAnalysis by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var quotaDialogMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollState = rememberScrollState()

    // 每次打开抽屉时重置到顶部
    LaunchedEffect(showAiSheet) {
        if (showAiSheet) {
            sheetScrollState.scrollTo(0)
        }
    }

    // 计算本月数据
    val thisMonthExpenses = remember(expenses) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        expenses.filter { it.date >= startOfMonth }
    }

    // 按类别统计
    val categoryStats = remember(thisMonthExpenses, categories) {
        thisMonthExpenses.groupBy { it.categoryId }
            .map { (categoryId, expenseList) ->
                val category = categories.find { it.id == categoryId }
                val total = expenseList.sumOf { it.amount }
                CategoryStat(
                    category = category,
                    total = total,
                    count = expenseList.size,
                    percentage = if (monthlyTotal > 0) (total / monthlyTotal * 100) else 0.0
                )
            }
            .sortedByDescending { it.total }
    }

    // 按日期统计（最近7天）
    val dailyStats = remember(thisMonthExpenses) {
        val dailyMap = mutableMapOf<String, Double>()

        // 初始化最近7天
        for (i in 6 downTo 0) {
            val date = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = SimpleDateFormat("MM-dd", Locale.CHINA).format(date.time)
            dailyMap[dateStr] = 0.0
        }

        // 统计实际消费
        thisMonthExpenses.forEach { expense ->
            val dateStr = SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(expense.date))
            if (dateStr in dailyMap) {
                dailyMap[dateStr] = dailyMap[dateStr]!! + expense.amount
            }
        }

        dailyMap.toList().sortedBy { it.first }
    }

    val canGenerateAnalysis = thisMonthExpenses.isNotEmpty()
    val currentYearMonth = remember {
        val calendar = Calendar.getInstance()
        "%d-%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
    val thisMonthIncome = remember(allIncome, currentYearMonth) {
        allIncome.firstOrNull { it.yearMonth == currentYearMonth }
    }

    val runAiAnalysis: () -> Unit = runAiAnalysis@{
        if (isGuest) {
            aiAnalysis = aiFeatureLockedLabel
            return@runAiAnalysis
        }
        if (!canGenerateAnalysis) {
            return@runAiAnalysis
        }
        scope.launch {
            isAnalyzing = true
            val summaries = thisMonthExpenses.map { expense ->
                val categoryName = categories.find { it.id == expense.categoryId }
                    ?.displayNameNonComposable(context)
                    ?: categoryOtherLabel
                AiExpenseParser.ExpenseSummary(amount = expense.amount, category = categoryName)
            }
            val result = AiExpenseParser.analyzeExpenses(expenses = summaries, month = thisMonthLabel, lang = aiLang)
            result.onSuccess { analysis -> aiAnalysis = analysis }
            result.onFailure { error ->
                val message = error.message ?: ""
                val isQuotaError = message.lowercase().contains("limit reached") ||
                    message.lowercase().contains("upgrade to pro")
                if (isQuotaError) {
                    showAiSheet = false
                    quotaDialogMessage = message
                    showQuotaDialog = true
                } else {
                    aiAnalysis = aiAnalysisErrorLabel
                }
            }
            isAnalyzing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.analysis_title),
                        fontSize = 28.sp,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(4.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd)),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!isGuest) showAiSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        PurpleStart.copy(alpha = 0.1f),
                                        PurpleEnd.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleStart)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.analysis_ai_entry_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isGuest) {
                                        stringResource(R.string.ai_feature_locked)
                                    } else {
                                        stringResource(R.string.analysis_ai_entry_subtitle)
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isGuest && onNavigateToLogin != null) {
                                    TextButton(onClick = onNavigateToLogin) {
                                        Text(stringResource(R.string.ai_go_login))
                                    }
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PurpleStart
                            )
                        }
                    }
                }
            }

        // 统计卡片
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.analysis_total_spent),
                    value = "$currencySymbol%.2f".format(monthlyTotal),
                    icon = Icons.Default.ShoppingCart,
                    color = PurpleStart,
                    extraContent = {
                        thisMonthIncome?.let { income ->
                            val balance = income.amount - monthlyTotal
                            val balanceColor = if (balance >= 0) IncomeGreen else ExpenseRed
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "收入 ${String.format(Locale.getDefault(), "%s%.2f", currencySymbol, income.amount)} · 支出 ${String.format(Locale.getDefault(), "%s%.2f", currencySymbol, monthlyTotal)} · ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "结余 ${String.format(Locale.getDefault(), "%s%.2f", currencySymbol, balance)}",
                                    fontSize = 11.sp,
                                    color = balanceColor
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = stringResource(R.string.analysis_transaction_count),
                    value = "${thisMonthExpenses.size}",
                    icon = Icons.Default.Receipt,
                    color = PurpleEnd,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (monthlyBudget != null && monthlyBudget > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.analysis_budget_overview),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BudgetDonutChart(
                                spent = monthlyTotal,
                                budget = monthlyBudget,
                                modifier = Modifier.size(140.dp)
                            )
                            Spacer(Modifier.width(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                BudgetStatRow(
                                    label = stringResource(R.string.analysis_budget_spent_label),
                                    value = "$currencySymbol%.2f".format(monthlyTotal),
                                    color = ExpenseRed
                                )
                                Spacer(Modifier.height(8.dp))
                                BudgetStatRow(
                                    label = stringResource(R.string.analysis_budget_total_label),
                                    value = "$currencySymbol%.2f".format(monthlyBudget),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                BudgetStatRow(
                                    label = stringResource(R.string.analysis_budget_remaining_label),
                                    value = "$currencySymbol%.2f".format(monthlyBudget - monthlyTotal),
                                    color = if (monthlyBudget > monthlyTotal) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.analysis_avg_daily),
                    value = "$currencySymbol%.0f".format(
                        if (thisMonthExpenses.isNotEmpty())
                            monthlyTotal / Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        else 0.0
                    ),
                    icon = Icons.Default.CalendarToday,
                    color = WarningOrange,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = stringResource(R.string.analysis_largest_category),
                    value = categoryStats.firstOrNull()?.category?.displayName() ?: "-",
                    icon = Icons.Default.Category,
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 最近7天趋势图
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.analysis_trend_title),
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (dailyStats.isNotEmpty()) {
                        DailyTrendChart(dailyStats)
                    } else {
                        Text(
                            text = stringResource(R.string.common_no_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
        }

        // 类别消费排行
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.analysis_category_rank_title),
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isEmpty()) {
                        Text(
                            text = stringResource(R.string.common_no_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    } else {
                        categoryStats.forEach { stat ->
                            CategoryStatItem(stat = stat, currencySymbol = currencySymbol)
                            if (stat != categoryStats.last()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Button(
                    onClick = onNavigateToStock,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.analysis_view_stock), color = Color.White)
                }
            }
        }

        }

        if (showAiSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAiSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(sheetScrollState)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PurpleStart,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.analysis_ai_entry_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isAnalyzing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PurpleStart, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.analysis_ai_loading),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (!aiAnalysis.isNullOrBlank()) {
                        Text(
                            text = aiAnalysis.orEmpty(),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    aiAnalysis = null
                                    runAiAnalysis()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.analysis_ai_regenerate))
                            }

                            Button(
                                onClick = { showAiSheet = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(stringResource(R.string.common_done))
                            }
                        }
                    } else {
                        if (thisMonthExpenses.isEmpty()) {
                            Text(
                                text = stringResource(R.string.analysis_ai_sheet_empty_message),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.analysis_ai_sheet_ready_message,
                                    thisMonthExpenses.size,
                                    "$currencySymbol${String.format("%.2f", monthlyTotal)}"
                                ),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = runAiAnalysis,
                            enabled = canGenerateAnalysis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleStart)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.analysis_ai_generate), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        if (showQuotaDialog) {
            AlertDialog(
                onDismissRequest = { showQuotaDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text(stringResource(R.string.ai_quota_dialog_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (quotaDialogMessage.lowercase().contains("daily") ||
                                quotaDialogMessage.lowercase().contains("每日") ||
                                quotaDialogMessage.lowercase().contains("每日")
                            ) {
                                stringResource(R.string.ai_quota_parse_daily, 10)
                            } else {
                                stringResource(R.string.ai_quota_analyze_monthly, 2)
                            }
                        )
                        Text(
                            text = stringResource(R.string.ai_quota_dialog_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQuotaDialog = false }) {
                        Text(stringResource(R.string.ai_quota_got_it))
                    }
                }
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    extraContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleLarge,
                color = color
            )

            extraContent?.invoke()
        }
    }
}

@Composable
fun DailyTrendChart(dailyStats: List<Pair<String, Double>>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val xLabels = remember(dailyStats) { dailyStats.map { it.first } }

    val yAxisFormatter = remember {
        CartesianValueFormatter { value, _, _ ->
            if (value >= 1000.0) {
                String.format(Locale.US, "%.1fk", value / 1000.0)
            } else {
                value.roundToInt().toString()
            }
        }
    }

    val xAxisFormatter = remember(xLabels) {
        CartesianValueFormatter { value, _, _ ->
            val label = xLabels.getOrNull(value.roundToInt()) ?: return@CartesianValueFormatter ""
            label.substringAfter('-', label)
        }
    }

    LaunchedEffect(dailyStats) {
        modelProducer.runTransaction {
            columnSeries {
                series(dailyStats.map { it.second })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = rememberStartAxis(valueFormatter = yAxisFormatter),
            bottomAxis = rememberBottomAxis(valueFormatter = xAxisFormatter)
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
fun CategoryStatItem(stat: CategoryStat, currencySymbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = getCategoryIcon(stat.category?.iconName),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = stat.category?.displayName() ?: stringResource(R.string.analysis_unknown_category),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.analysis_expense_count, stat.count),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$currencySymbol%.2f".format(stat.total),
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "%.1f%%".format(stat.percentage),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 进度条
    LinearProgressIndicator(
        progress = { (stat.percentage / 100).toFloat() },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = PurpleStart,
    )
}

data class CategoryStat(
    val category: Category?,
    val total: Double,
    val count: Int,
    val percentage: Double
)

@Composable
fun BudgetDonutChart(
    spent: Double,
    budget: Double,
    modifier: Modifier = Modifier
) {
    val percentage = (spent / budget).coerceIn(0.0, 1.0).toFloat()
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(1000),
        label = "budget_arc"
    )

    val color = when {
        percentage >= 1.0f -> ExpenseRed
        percentage >= 0.8f -> WarningOrange
        else -> PurpleStart
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedPercentage,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = stringResource(R.string.analysis_budget_used),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BudgetStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

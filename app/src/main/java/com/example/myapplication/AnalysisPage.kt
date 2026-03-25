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
    categories: List<Category> = emptyList()
) {
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val scope = rememberCoroutineScope()
    var aiAnalysis by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
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

    val runAiAnalysis: () -> Unit = runAiAnalysis@{
        if (!canGenerateAnalysis) {
            return@runAiAnalysis
        }
        scope.launch {
            isAnalyzing = true
            val summaries = thisMonthExpenses.map { expense ->
                val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "其他"
                AiExpenseParser.ExpenseSummary(amount = expense.amount, category = categoryName)
            }
            val result = AiExpenseParser.analyzeExpenses(expenses = summaries, month = "本月")
            result.onSuccess { analysis -> aiAnalysis = analysis }
            result.onFailure { aiAnalysis = "分析生成失败，请检查网络连接" }
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
                        text = "消费分析",
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
                        .clickable { showAiSheet = true },
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
                                    Text("AI 深度消费报告", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "让 AI 为你诊断本月的财务健康状况",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                    title = "本月支出",
                    value = "¥%.2f".format(monthlyTotal),
                    icon = Icons.Default.ShoppingCart,
                    color = PurpleStart,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "消费笔数",
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
                            text = "预算概览",
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
                                    label = "已支出",
                                    value = "¥%.2f".format(monthlyTotal),
                                    color = ExpenseRed
                                )
                                Spacer(Modifier.height(8.dp))
                                BudgetStatRow(
                                    label = "总预算",
                                    value = "¥%.2f".format(monthlyBudget),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                BudgetStatRow(
                                    label = "剩余",
                                    value = "¥%.2f".format(monthlyBudget - monthlyTotal),
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
                    title = "日均消费",
                    value = "¥%.0f".format(
                        if (thisMonthExpenses.isNotEmpty())
                            monthlyTotal / Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        else 0.0
                    ),
                    icon = Icons.Default.CalendarToday,
                    color = WarningOrange,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "最大类别",
                    value = categoryStats.firstOrNull()?.category?.name ?: "-",
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
                        text = "最近7天趋势",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (dailyStats.isNotEmpty()) {
                        DailyTrendChart(dailyStats)
                    } else {
                        Text(
                            text = "暂无数据",
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
                        text = "类别消费排行",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isEmpty()) {
                        Text(
                            text = "暂无数据",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    } else {
                        categoryStats.forEach { stat ->
                            CategoryStatItem(stat)
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
                    Text("查看股票持仓", color = Color.White)
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
                        Text("AI 消费洞察", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                                "AI 正在分析你的账单...",
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
                                Text("重新分析")
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
                                Text("完成阅读")
                            }
                        }
                    } else {
                        if (thisMonthExpenses.isEmpty()) {
                            Text(
                                text = "本月还没有消费记录。建议先记几笔账，DeepSeek 才能为你生成准确的财务诊断报告哦！",
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "将基于本月已记录的 ${thisMonthExpenses.size} 笔支出（共 ¥%.2f）生成专属的财务诊断报告。AI 会帮你发现潜在的浪费，并提供优化建议。".format(monthlyTotal),
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
                            Text("开始生成报告", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
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
        }
    }
}

@Composable
fun DailyTrendChart(dailyStats: List<Pair<String, Double>>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val xLabels = remember(dailyStats) { dailyStats.map { it.first } }

    val yAxisFormatter = remember {
        CartesianValueFormatter { value, _, _ ->
            "HK$${value.roundToInt()}"
        }
    }

    val xAxisFormatter = remember(xLabels) {
        CartesianValueFormatter { value, _, _ ->
            xLabels.getOrNull(value.roundToInt()) ?: ""
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
fun CategoryStatItem(stat: CategoryStat) {
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
                    text = stat.category?.name ?: "未知",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${stat.count} 笔消费",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "¥%.2f".format(stat.total),
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
                text = "已使用",
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

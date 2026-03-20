package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.data.SavingGoal
import com.example.myapplication.data.Stock
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun HomePage(viewModel: ExpenseViewModel,
             onNavigateToEdit: (Expense) -> Unit = {},
             onNavigateToRecord: () -> Unit = {},
             onNavigateToSaving: () -> Unit = {},
             onNavigateToStock: () -> Unit = {},
             monthlyBudget: Double? = null,
             savingGoals: List<SavingGoal> = emptyList(),
             stocks: List<Stock> = emptyList()
) {
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("全部") }

    // 根据筛选条件过滤消费记录
    val filteredExpenses = remember(expenses, selectedFilter) {
        when (selectedFilter) {
            "今日" -> {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= todayStart }
            }
            "本周" -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= weekStart }
            }
            "本月" -> {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "消费概览",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
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
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PurpleStart, PurpleEnd)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text("本月支出", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(
                    "¥ %.2f".format(monthlyTotal),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroStatItem("今日", "¥%.0f".format(todayTotal))
                    HeroStatItem("本周", "¥%.0f".format(weeklyTotal))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 预算进度网格（紧凑样式）
        if (monthlyBudget != null && monthlyBudget > 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BudgetProgressCard(
                    monthlyTotal = monthlyTotal,
                    monthlyBudget = monthlyBudget,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        val ongoingSavingGoals = remember(savingGoals) {
            savingGoals.filter { !it.isCompleted }
        }

        if (ongoingSavingGoals.isNotEmpty()) {
            SavingGoalSummaryCard(
                ongoingGoals = ongoingSavingGoals,
                onClick = onNavigateToSaving
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (stocks.isNotEmpty()) {
            StockOverviewCard(
                stocks = stocks,
                onClick = onNavigateToStock
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ===== 筛选按钮 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("今日", "本周", "本月", "全部").forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
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
                                        brush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                                        shape = RoundedCornerShape(50)
                                    )
                            } else {
                                Modifier
                            }
                        ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent,
                        selectedLabelColor = Color.White
                    )
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
                text = "最近消费",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${filteredExpenses.size} 条记录",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 消费记录列表
        if (expenses.isEmpty()) {
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
                        text = "还没有消费记录",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击底部「记账」开始记录你的消费吧",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        Text("开始记账")
                    }
                }
            }
        } else {
            // 消费列表
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredExpenses.forEach { expense ->
                    val category = categories.find { it.id == expense.categoryId }
                    ExpenseItem(
                        expense = expense,
                        category = category,
                        onDelete = {
                            scope.launch {
                                viewModel.deleteExpense(expense)
                            }
                        },
                        onEdit = {
                            onNavigateToEdit(expense)  // 点击卡片时导航到编辑页面
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StockOverviewCard(
    stocks: List<Stock>,
    onClick: () -> Unit
) {
    val totalValue = remember(stocks) { stocks.sumOf { it.currentPrice * it.shares } }
    val totalProfit = remember(stocks) { stocks.sumOf { (it.currentPrice - it.costPrice) * it.shares } }
    val profitColor = if (totalProfit >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "股票持仓",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 ${stocks.size} 只",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "总市值 HK$${String.format(Locale.getDefault(), "%.2f", totalValue)}",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "总盈亏 ${formatSignedHkAmount(totalProfit)}",
                fontSize = 14.sp,
                color = profitColor
            )
        }
    }
}

private fun formatSignedHkAmount(amount: Double): String {
    val sign = if (amount >= 0) "+" else "-"
    return "${sign}HK$${String.format(Locale.getDefault(), "%.2f", kotlin.math.abs(amount))}"
}

@Composable
private fun SavingGoalSummaryCard(
    ongoingGoals: List<SavingGoal>,
    onClick: () -> Unit
) {
    val latestGoal = remember(ongoingGoals) { ongoingGoals.maxByOrNull { it.createdAt } } ?: return
    val progress = (latestGoal.currentAmount / latestGoal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val percentage = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "储蓄目标",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 ${ongoingGoals.size} 个目标",
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
                    String.format(Locale.getDefault(), "¥ %.2f", latestGoal.currentAmount),
                    String.format(Locale.getDefault(), "¥ %.2f", latestGoal.targetAmount)
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
                    text = "还有 ${ongoingGoals.size - 1} 个目标进行中",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    category: Category?,
    onDelete: () -> Unit,
    onEdit: () -> Unit  // 添加编辑回调
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
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
                // 类别图标（优先显示自定义图片）
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
                        text = category?.name ?: "未知类别",
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = DateUtils.formatDate(expense.date),
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

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "¥ %.2f".format(expense.amount),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = ExpenseRed
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
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
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预算进度",
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

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            // 详细信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (remaining >= 0) "还剩" else "超支",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${"%.2f".format(kotlin.math.abs(remaining))}",
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
                            text = "每日可用",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${"%.2f".format(dailyAvailable)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "距月底还有 $remainingDays 天",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
// 格式化日期
// 格式化日期
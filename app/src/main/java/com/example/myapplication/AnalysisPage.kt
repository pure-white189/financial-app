package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisPage(viewModel: ExpenseViewModel) {
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()

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
        val calendar = Calendar.getInstance()
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "消费分析",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge
            )
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "消费笔数",
                    value = "${thisMonthExpenses.size}",
                    icon = Icons.Default.Receipt,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
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
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "最大类别",
                    value = categoryStats.firstOrNull()?.category?.name ?: "-",
                    icon = Icons.Default.Category,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 最近7天趋势图
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "最近7天趋势",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "类别消费排行",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium
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
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis()
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
    )
}

data class CategoryStat(
    val category: Category?,
    val total: Double,
    val count: Int,
    val percentage: Double
)
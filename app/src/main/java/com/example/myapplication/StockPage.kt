package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Stock
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import com.example.myapplication.ui.theme.StockGreen
import com.example.myapplication.ui.theme.StockRed
import com.example.myapplication.ui.theme.WarningOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val ProfitColor = IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockPage(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val stocks by viewModel.stocks.collectAsState(initial = emptyList())
    val totalStockValue by viewModel.totalStockValue.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Stock?>(null) }
    var updateTarget by remember { mutableStateOf<Stock?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val totalProfit = stocks.sumOf { (it.currentPrice - it.costPrice) * it.shares }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("股票追踪") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isRefreshing,
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                // TODO: 对接真实股票行情 API 后，在这里替换为真实接口刷新
                                stocks.forEach { stock ->
                                    val change = (Random.nextDouble() * 0.10) - 0.05
                                    val newPrice = stock.currentPrice * (1 + change)
                                    viewModel.updateStockPrice(stock, newPrice)
                                }
                                delay(1000)
                                isRefreshing = false
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                        shape = CircleShape
                    )
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color.Transparent
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "添加股票", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleStart.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "总市值", fontSize = 13.sp)
                        Text(
                            text = formatMoney("HK", totalStockValue),
                            fontSize = 20.sp,
                            color = PurpleStart
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "总盈亏", fontSize = 13.sp)
                        Text(
                            text = formatSignedMoney("HK", totalProfit),
                            fontSize = 20.sp,
                            color = if (totalProfit >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (stocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "还没有追踪的股票",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加股票")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stocks, key = { it.id }) { stock ->
                        StockItemCard(
                            stock = stock,
                            onDelete = { deleteTarget = stock },
                            onUpdatePrice = { updateTarget = stock }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { stock ->
                viewModel.addStock(stock)
                showAddDialog = false
            }
        )
    }

    deleteTarget?.let { stock ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除股票") },
            text = { Text("确定要删除「${stock.symbol} ${stock.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStock(stock)
                        deleteTarget = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }

    updateTarget?.let { stock ->
        UpdatePriceDialog(
            stock = stock,
            onDismiss = { updateTarget = null },
            onConfirm = { newPrice ->
                viewModel.updateStockPrice(stock, newPrice)
                updateTarget = null
            }
        )
    }
}

@Composable
private fun StockItemCard(
    stock: Stock,
    onDelete: () -> Unit,
    onUpdatePrice: () -> Unit
) {
    val diffAmount = stock.currentPrice - stock.costPrice
    val diffPercent = if (stock.costPrice > 0) (diffAmount / stock.costPrice * 100) else 0.0
    val pnl = diffAmount * stock.shares
    val value = stock.currentPrice * stock.shares
    val upColor = if (diffAmount >= 0) StockGreen else StockRed

    val marketColor = when (stock.market) {
        "HK" -> PurpleStart
        "US" -> WarningOrange
        else -> ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text(stock.market, color = Color.White) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = marketColor,
                                labelColor = Color.White
                            )
                        )
                    }
                    Text(
                        text = stock.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(stock.market, stock.currentPrice),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${formatSignedMoney(stock.market, diffAmount)} (${formatSignedPercent(diffPercent)})",
                        fontSize = 12.sp,
                        color = upColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "持股 ${formatDouble(stock.shares)} | 成本 ${formatMoney(stock.market, stock.costPrice)} | 市值 ${formatMoney(stock.market, value)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "持仓盈亏 ${formatSignedMoney(stock.market, pnl)}",
                fontSize = 12.sp,
                color = if (pnl >= 0) IncomeGreen else ExpenseRed
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onUpdatePrice) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("更新价格")
                }

                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun AddStockDialog(
    onDismiss: () -> Unit,
    onConfirm: (Stock) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var market by remember { mutableStateOf("HK") }
    var sharesText by remember { mutableStateOf("") }
    var costPriceText by remember { mutableStateOf("") }
    var currentPriceText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加股票") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = {
                        symbol = it.uppercase()
                        showError = false
                    },
                    label = { Text("股票代码") },
                    placeholder = { Text("如：0700 / AAPL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("股票名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HK", "US", "CN").forEach { marketOption ->
                        FilterChip(
                            selected = market == marketOption,
                            onClick = { market = marketOption },
                            label = { Text(marketOption) }
                        )
                    }
                }

                OutlinedTextField(
                    value = sharesText,
                    onValueChange = {
                        sharesText = it
                        showError = false
                    },
                    label = { Text("持股数量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = {
                        costPriceText = it
                        if (currentPriceText.isBlank()) {
                            currentPriceText = it
                        }
                        showError = false
                    },
                    label = { Text("买入均价") },
                    prefix = { Text(marketPrefix(market)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = {
                        currentPriceText = it
                        showError = false
                    },
                    label = { Text("当前价格") },
                    prefix = { Text(marketPrefix(market)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = "请完整填写并输入有效数值",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val shares = sharesText.toDoubleOrNull()
                    val costPrice = costPriceText.toDoubleOrNull()
                    val currentPrice = currentPriceText.toDoubleOrNull()

                    if (symbol.isBlank() || name.isBlank() || shares == null || shares <= 0 ||
                        costPrice == null || costPrice <= 0 || currentPrice == null || currentPrice <= 0
                    ) {
                        showError = true
                        return@TextButton
                    }

                    onConfirm(
                        Stock(
                            symbol = symbol.trim(),
                            name = name.trim(),
                            shares = shares,
                            costPrice = costPrice,
                            currentPrice = currentPrice,
                            lastUpdated = System.currentTimeMillis(),
                            market = market
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun UpdatePriceDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceText by remember(stock.id) { mutableStateOf(String.format("%.2f", stock.currentPrice)) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新价格 - ${stock.symbol}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        showError = false
                    },
                    label = { Text("当前价格") },
                    prefix = { Text(marketPrefix(stock.market)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                if (showError) {
                    Text(
                        text = "请输入大于 0 的价格",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newPrice = priceText.toDoubleOrNull()
                    if (newPrice == null || newPrice <= 0) {
                        showError = true
                        return@TextButton
                    }
                    onConfirm(newPrice)
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun marketPrefix(market: String): String {
    return when (market) {
        "HK" -> "HK$"
        "US" -> "$"
        else -> "¥"
    }
}

private fun formatMoney(market: String, amount: Double): String {
    return "${marketPrefix(market)} ${String.format("%.2f", amount)}"
}

private fun formatSignedMoney(market: String, amount: Double): String {
    val sign = if (amount > 0) "+" else ""
    return "$sign${marketPrefix(market)} ${String.format("%.2f", amount)}"
}

private fun formatSignedPercent(percent: Double): String {
    return String.format("%+.2f%%", percent)
}

private fun formatDouble(value: Double): String {
    return String.format("%.2f", value)
}


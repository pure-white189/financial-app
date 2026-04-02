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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Stock
import com.example.myapplication.data.AiExpenseParser
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import com.example.myapplication.ui.theme.StockGreen
import com.example.myapplication.ui.theme.StockRed
import com.example.myapplication.ui.theme.WarningOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ProfitColor = IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockPage(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stocks by viewModel.stocks.collectAsState(initial = emptyList())
    val totalStockValue by viewModel.totalStockValue.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Stock?>(null) }
    var editTarget by remember { mutableStateOf<Stock?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastRefreshTime by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val totalProfit = stocks.sumOf { (it.currentPrice - it.costPrice) * it.shares }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stock_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isRefreshing,
                        onClick = {
                            scope.launch {
                                isRefreshing = true

                                if (stocks.isNotEmpty()) {
                                    val symbols = stocks.map { stock ->
                                        toApiSymbol(stock.symbol, stock.market)
                                    }

                                    val result = AiExpenseParser.fetchStockPrices(symbols)
                                    val priceMap = result.getOrNull()

                                    if (priceMap != null) {
                                        var updatedCount = 0
                                        stocks.forEachIndexed { index, stock ->
                                            val symbol = symbols[index]
                                            priceMap[symbol]?.let { stockPrice ->
                                                if (stockPrice.price > 0) {
                                                    viewModel.updateStockPrice(stock, stockPrice.price)
                                                    updatedCount++
                                                }
                                            }
                                        }

                                        if (updatedCount > 0) {
                                            lastRefreshTime = SimpleDateFormat(
                                                "HH:mm",
                                                Locale.getDefault()
                                            ).format(Date())
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.stock_no_price_available))
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar(context.getString(R.string.stock_fetch_failed))
                                    }
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
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.stock_refresh))
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
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.stock_add), tint = Color.White)
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
                        Text(text = stringResource(R.string.stock_total_value), fontSize = 13.sp)
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
                        Text(text = stringResource(R.string.stock_total_profit), fontSize = 13.sp)
                        Text(
                            text = formatSignedMoney("HK", totalProfit),
                            fontSize = 20.sp,
                            color = if (totalProfit >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (lastRefreshTime.isNotBlank()) {
                Text(
                    text = stringResource(R.string.stock_last_updated_with_time, lastRefreshTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                            text = stringResource(R.string.stock_no_holdings),
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
                            Text(stringResource(R.string.stock_add))
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
                            onEdit = { editTarget = stock },
                            onRefreshPrice = { target ->
                                val symbol = toApiSymbol(target.symbol, target.market)
                                val result = AiExpenseParser.fetchStockPrices(listOf(symbol))
                                val stockPrice = result.getOrNull()?.get(symbol)
                                if (stockPrice != null && stockPrice.price > 0) {
                                    viewModel.updateStockPrice(target, stockPrice.price)
                                    lastRefreshTime = SimpleDateFormat(
                                        "HH:mm",
                                        Locale.getDefault()
                                    ).format(Date())
                                    true
                                } else {
                                    false
                                }
                            },
                            onShowMessage = { message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newStock ->
                viewModel.addStock(newStock)
                showAddDialog = false

                if (newStock.currentPrice == 0.0) {
                    scope.launch {
                        val targetStock = run {
                            var found: Stock? = null
                            repeat(10) {
                                found = stocks.firstOrNull {
                                    it.createdAt == newStock.createdAt &&
                                        it.symbol == newStock.symbol &&
                                        it.market == newStock.market
                                }
                                if (found == null) {
                                    delay(120)
                                }
                            }
                            found
                        }

                        if (targetStock != null) {
                            val symbol = toApiSymbol(targetStock.symbol, targetStock.market)
                            val result = AiExpenseParser.fetchStockPrices(listOf(symbol))
                            result.onSuccess { priceMap ->
                                priceMap[symbol]?.let { stockPrice ->
                                    if (stockPrice.price > 0) {
                                        viewModel.updateStockPrice(targetStock, stockPrice.price)
                                        lastRefreshTime = SimpleDateFormat(
                                            "HH:mm",
                                            Locale.getDefault()
                                        ).format(Date())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    deleteTarget?.let { stock ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.stock_delete_confirm)) },
            text = { Text(stringResource(R.string.stock_delete_with_name, stock.symbol, stock.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStock(stock)
                        deleteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    editTarget?.let { stock ->
        EditHoldingDialog(
            stock = stock,
            onDismiss = { editTarget = null },
            onConfirm = { updated ->
                viewModel.updateStock(updated)
                editTarget = null
            }
        )
    }
}

@Composable
private fun StockItemCard(
    stock: Stock,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onRefreshPrice: suspend (Stock) -> Boolean,
    onShowMessage: (String) -> Unit
) {
    val diffAmount = stock.currentPrice - stock.costPrice
    val diffPercent = if (stock.costPrice > 0) (diffAmount / stock.costPrice * 100) else 0.0
    val pnl = diffAmount * stock.shares
    val value = stock.currentPrice * stock.shares
    val upColor = if (diffAmount >= 0) StockGreen else StockRed

    val marketColor = when (stock.market) {
        "HK" -> PurpleStart
        "US" -> WarningOrange
        "SS" -> ExpenseRed
        "SZ" -> ExpenseRed
        else -> ExpenseRed
    }
    val marketLabel = when (stock.market) {
        "SS" -> stringResource(R.string.stock_market_ss)
        "SZ" -> stringResource(R.string.stock_market_sz)
        else -> stock.market
    }
    val stockUpdatedText = stringResource(R.string.stock_updated)
    val stockFetchFailedText = stringResource(R.string.stock_fetch_failed)
    val scope = rememberCoroutineScope()
    var isSingleRefreshing by remember(stock.id) { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            label = { Text(marketLabel, color = Color.White) },
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

                Row(verticalAlignment = Alignment.Top) {
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

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.common_settings)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (isSingleRefreshing) stringResource(R.string.common_loading) else stringResource(R.string.stock_refresh))
                                },
                                enabled = !isSingleRefreshing,
                                onClick = {
                                    expanded = false
                                    scope.launch {
                                        isSingleRefreshing = true
                                        val updated = onRefreshPrice(stock)
                                        isSingleRefreshing = false
                                        onShowMessage(if (updated) stockUpdatedText else stockFetchFailedText)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stock_update_price)) },
                                onClick = {
                                    expanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.common_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(
                    R.string.stock_holding_summary,
                    formatDouble(stock.shares),
                    formatMoney(stock.market, stock.costPrice),
                    formatMoney(stock.market, value)
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.stock_profit_loss_summary, formatSignedMoney(stock.market, pnl)),
                fontSize = 12.sp,
                color = if (pnl >= 0) IncomeGreen else ExpenseRed
            )
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
    val canSave = symbol.isNotBlank() &&
        name.isNotBlank() &&
        (sharesText.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stock_add)) },
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
                    label = { Text(stringResource(R.string.stock_symbol_hint)) },
                    placeholder = { Text(stringResource(R.string.stock_symbol_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.stock_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "HK" to "HK",
                        "US" to "US",
                        "SS" to stringResource(R.string.stock_market_ss),
                        "SZ" to stringResource(R.string.stock_market_sz)
                    ).forEach { (marketOption, label) ->
                        FilterChip(
                            selected = market == marketOption,
                            onClick = { market = marketOption },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = sharesText,
                    onValueChange = {
                        sharesText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.stock_shares_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = {
                        costPriceText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.stock_cost_hint)) },
                    placeholder = { Text(stringResource(R.string.stock_cost_optional_hint)) },
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
                    label = { Text(stringResource(R.string.stock_current_price)) },
                    placeholder = { Text(stringResource(R.string.stock_current_optional_hint)) },
                    prefix = { Text(marketPrefix(market)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.stock_validation_error),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val shares = sharesText.toDoubleOrNull()
                    val costPrice = costPriceText.toDoubleOrNull() ?: 0.0
                    val currentPrice = currentPriceText.toDoubleOrNull() ?: 0.0

                    if (symbol.isBlank() || name.isBlank() || shares == null || shares <= 0) {
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
                Text(stringResource(R.string.common_save))
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
private fun EditHoldingDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Stock) -> Unit
) {
    var name by remember(stock.id) { mutableStateOf(stock.name) }
    var sharesText by remember(stock.id) { mutableStateOf(String.format("%.2f", stock.shares)) }
    var costPriceText by remember(stock.id) {
        mutableStateOf(if (stock.costPrice > 0) String.format("%.2f", stock.costPrice) else "")
    }
    var currentPriceText by remember(stock.id) {
        mutableStateOf(if (stock.currentPrice > 0) String.format("%.2f", stock.currentPrice) else "")
    }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stock_edit_holding_title, stock.symbol)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.stock_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                OutlinedTextField(
                    value = sharesText,
                    onValueChange = {
                        sharesText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.stock_shares_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = {
                        costPriceText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.stock_cost_hint)) },
                    placeholder = { Text(stringResource(R.string.stock_cost_optional_hint)) },
                    prefix = { Text(marketPrefix(stock.market)) },
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
                    label = { Text(stringResource(R.string.stock_latest_price_hint)) },
                    placeholder = { Text(stringResource(R.string.stock_latest_price_placeholder)) },
                    prefix = { Text(marketPrefix(stock.market)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.stock_edit_validation_error),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newShares = sharesText.toDoubleOrNull()
                    val newCostPrice = costPriceText.toDoubleOrNull() ?: 0.0
                    val newCurrentPrice = currentPriceText.toDoubleOrNull() ?: stock.currentPrice
                    if (name.isBlank() || newShares == null || newShares <= 0) {
                        showError = true
                        return@TextButton
                    }
                    onConfirm(
                        stock.copy(
                            name = name.trim(),
                            shares = newShares,
                            costPrice = newCostPrice,
                            currentPrice = newCurrentPrice
                        )
                    )
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

private fun toApiSymbol(symbol: String, market: String): String {
    val cleaned = symbol.trim().uppercase()
    return when (market) {
        // 港股：0700.HK（腾讯）、9988.HK（阿里）
        "HK" -> "${cleaned.padStart(4, '0')}.HK"

        // 美股：AAPL、TSLA、MSFT
        "US" -> cleaned

        // A股上交所：600519.SS（茅台）
        // A股深交所：000858.SZ（五粮液）
        "SS" -> "$cleaned.SS"
        "SZ" -> "$cleaned.SZ"
        "CN" -> if (cleaned.startsWith("6")) "$cleaned.SS" else "$cleaned.SZ"

        else -> cleaned
    }
}


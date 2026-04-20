package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.data.getCurrencySymbol
import com.example.myapplication.data.ThemePreferences
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpensePage(
    expense: Expense,
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = remember(context) { ThemePreferences(context) }
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val mainCurrencyCode by viewModel.mainCurrencyCode.collectAsState()
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf(expense.note) }
    var selectedDate by remember { mutableStateOf(expense.date) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(
        expense.originalCurrency ?: mainCurrencyCode
    ) }
    var exchangeRate by remember { mutableStateOf<Double?>(expense.exchangeRate) }
    var isLoadingRate by remember { mutableStateOf(false) }
    var rateEditable by remember { mutableStateOf(false) }
    var rateInputText by remember { mutableStateOf(
        expense.exchangeRate?.toString() ?: ""
    ) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val currencySymbol = getCurrencySymbol(mainCurrencyCode)

    // 初始化选中的类别
    LaunchedEffect(categories) {
        if (selectedCategory == null) {
            selectedCategory = categories.find { it.id == expense.categoryId }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.record_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // 删除按钮
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 金额输入
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.debt_amount_hint)) },
                placeholder = { Text(stringResource(R.string.record_amount_hint)) },
                leadingIcon = { Text(currencySymbol , fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("HKD", "CNY", "USD").forEach { code ->
                    val isSelected = selectedCurrency == code
                    TextButton(
                        onClick = {
                            selectedCurrency = code
                            if (code == mainCurrencyCode) {
                                exchangeRate = null
                                rateEditable = false
                                rateInputText = ""
                                isLoadingRate = false
                            } else {
                                isLoadingRate = true
                                rateEditable = false
                                scope.launch {
                                    val rate = viewModel.fetchExchangeRate(code, mainCurrencyCode)
                                    if (rate != null) {
                                        exchangeRate = rate
                                        rateInputText = rate.toString()
                                        isLoadingRate = false
                                    } else {
                                        exchangeRate = null
                                        rateEditable = true
                                        rateInputText = ""
                                        isLoadingRate = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                    ) {
                        Text(code)
                    }
                }
            }

            if (selectedCurrency != mainCurrencyCode) {
                Spacer(modifier = Modifier.height(4.dp))
                if (isLoadingRate) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching rate...", fontSize = 12.sp)
                    }
                } else {
                    val enteredAmount = amount.toDoubleOrNull()
                    val effectiveRate = rateInputText.toDoubleOrNull() ?: exchangeRate
                    if (effectiveRate != null && enteredAmount != null) {
                        val convertedAmount = enteredAmount * effectiveRate
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1 $selectedCurrency = ", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.4f", effectiveRate), fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { rateEditable = !rateEditable })
                            Text(" $mainCurrencyCode  ->  $currencySymbol${String.format("%.2f", convertedAmount)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (rateEditable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rateInputText,
                            onValueChange = { rateInputText = it },
                            label = { Text("Exchange rate") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 日期时间选择
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDateTimePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.record_date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateUtils.formatDateForDisplay(context, selectedDate),
                            fontSize = 16.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.record_date),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 类别选择
            Text(
                text = stringResource(R.string.record_category),
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 备注输入
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.record_note_hint)) },
                placeholder = { Text(stringResource(R.string.record_note_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && selectedCategory != null) {
                        val effectiveRate = rateInputText.toDoubleOrNull() ?: exchangeRate
                        val finalAmount = if (selectedCurrency != mainCurrencyCode && effectiveRate != null) {
                            (amount.toDoubleOrNull() ?: 0.0) * effectiveRate
                        } else {
                            amount.toDoubleOrNull() ?: 0.0
                        }
                        val updatedExpense = expense.copy(
                            amount = finalAmount,
                            categoryId = selectedCategory!!.id,
                            date = selectedDate,
                            note = note,
                            originalAmount = if (selectedCurrency != mainCurrencyCode)
                                amount.toDoubleOrNull() else null,
                            originalCurrency = if (selectedCurrency != mainCurrencyCode)
                                selectedCurrency else null,
                            exchangeRate = if (selectedCurrency != mainCurrencyCode)
                                effectiveRate else null
                        )
                        scope.launch {
                            viewModel.updateExpense(updatedExpense)
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amount.toDoubleOrNull() != null &&
                        amount.toDoubleOrNull()!! > 0 &&
                        selectedCategory != null
            ) {
                Text(stringResource(R.string.record_save_changes), fontSize = 18.sp)
            }
        }
    }

    // 日期时间选择器
    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDateTime = selectedDate,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { newDateTime ->
                selectedDate = newDateTime
                showDateTimePicker = false
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.debt_delete_confirm)) },
            text = { Text(stringResource(R.string.record_delete_expense_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteExpense(expense)
                            onBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}



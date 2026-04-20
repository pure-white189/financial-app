package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ExpenseViewModel
import com.example.myapplication.R
import com.example.myapplication.data.MonthlyIncome
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomePage(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onFirstIncomeRecorded: () -> Unit = {},
) {
    val allIncome by viewModel.allIncome.collectAsState()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val savingGoals by viewModel.savingGoals.collectAsState(initial = emptyList())
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val currentMonthIncomeFlow = remember(viewModel) { viewModel.getIncomeForCurrentMonth() }
    val currentMonthIncome by currentMonthIncomeFlow.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentYearMonth = remember { viewModel.getCurrentYearMonth() }

    var showEditDialog by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    var transferAmount by remember { mutableStateOf<Double?>(null) }

    val expenseByMonth = remember(expenses) {
        expenses.groupBy { toYearMonth(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    fun openEditDialog(income: MonthlyIncome?) {
        amountInput = if (income != null) {
            String.format(Locale.getDefault(), "%.2f", income.amount)
        } else {
            ""
        }
        noteInput = income?.note.orEmpty()
        showEditDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.income_page_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.income_this_month),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { openEditDialog(currentMonthIncome) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.common_edit)
                            )
                        }
                    }

                    if (currentMonthIncome == null) {
                        Text(
                            text = stringResource(R.string.income_no_record),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = { openEditDialog(null) }) {
                            Text(text = stringResource(R.string.income_set_now))
                        }
                    } else {
                        Text(
                            text = formatAmount(currencySymbol, currentMonthIncome!!.amount),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        if (currentMonthIncome!!.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentMonthIncome!!.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.income_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (allIncome.isEmpty()) {
                Text(
                    text = stringResource(R.string.common_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allIncome, key = { it.yearMonth }) { income ->
                        val expenseAmount = expenseByMonth[income.yearMonth] ?: 0.0
                        val balance = income.amount - expenseAmount
                        val isPositive = balance >= 0
                        val balanceLabel = if (isPositive) {
                            stringResource(R.string.income_balance_positive)
                        } else {
                            stringResource(R.string.income_balance_negative)
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = income.yearMonth,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatAmount(currencySymbol, income.amount),
                                    color = IncomeGreen,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.income_expense_summary,
                                        formatAmount(currencySymbol, expenseAmount)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$balanceLabel ${formatAmount(currencySymbol, kotlin.math.abs(balance))}",
                                    color = if (isPositive) IncomeGreen else ExpenseRed,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (balance > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(onClick = { transferAmount = balance }) {
                                        Text(stringResource(R.string.income_transfer_to_goal))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        val parsedAmount = amountInput.toDoubleOrNull()

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.income_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text(stringResource(R.string.income_edit_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text(stringResource(R.string.income_edit_note_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsedAmount != null && parsedAmount >= 0,
                    onClick = {
                        viewModel.upsertIncome(currentYearMonth, parsedAmount ?: 0.0, noteInput)
                        onFirstIncomeRecorded()
                        showEditDialog = false
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    transferAmount?.let { amount ->
        AlertDialog(
            onDismissRequest = { transferAmount = null },
            title = { Text(stringResource(R.string.income_transfer_dialog_title)) },
            text = {
                if (savingGoals.isEmpty()) {
                    Text(stringResource(R.string.common_no_data))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savingGoals, key = { it.id }) { goal ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.depositToGoal(goal.id, amount)
                                    transferAmount = null
                                    val formattedBalance = formatAmount(currencySymbol, amount)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.income_transfer_success, formattedBalance)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = goal.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (savingGoals.isEmpty()) {
                    Button(onClick = { transferAmount = null }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { transferAmount = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

private fun toYearMonth(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "%d-%02d".format(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1
    )
}

private fun formatAmount(currencySymbol: String, amount: Double): String {
    return String.format(Locale.getDefault(), "%s %.2f", currencySymbol, amount)
}


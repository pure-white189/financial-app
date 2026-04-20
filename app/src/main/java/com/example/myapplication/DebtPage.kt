package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.data.Loan
import com.example.myapplication.data.getCurrencySymbol
import com.example.myapplication.ui.theme.ExpenseRed
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

private const val LOAN_TYPE_IN = "借入"
private const val LOAN_TYPE_OUT = "借出"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPage(
    viewModel: ExpenseViewModel,
    onFirstLoanCreated: () -> Unit = {},
) {
    val context = LocalContext.current
    val loans by viewModel.loans.collectAsState(initial = emptyList())
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val mainCurrencyCode by viewModel.mainCurrencyCode.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTargetLoan by remember { mutableStateOf<Loan?>(null) }

    val filteredLoans = loans.filter {
        when (selectedTabIndex) {
            1 -> it.type == LOAN_TYPE_OUT
            2 -> it.type == LOAN_TYPE_IN
            else -> true
        }
    }

    val unrepaidOut = loans.filter { it.type == LOAN_TYPE_OUT && !it.isRepaid }.sumOf { it.amount }
    val unrepaidIn = loans.filter { it.type == LOAN_TYPE_IN && !it.isRepaid }.sumOf { it.amount }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.debt_title)) }
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
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.debt_add_record), tint = Color.White)
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
                        Text(text = stringResource(R.string.debt_total_lend), fontSize = 13.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "$currencySymbol %.2f", unrepaidOut),
                            fontSize = 20.sp,
                            color = PurpleStart
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.debt_total_borrow), fontSize = 13.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "$currencySymbol %.2f", unrepaidIn),
                            fontSize = 20.sp,
                            color = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTabIndex) {
                listOf(
                    stringResource(R.string.home_filter_all),
                    stringResource(R.string.debt_lend),
                    stringResource(R.string.debt_borrow)
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLoans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.debt_no_records),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.debt_empty_hint),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        FilledTonalButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.debt_add_record))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLoans, key = { it.id }) { loan ->
                        LoanItemCard(
                            loan = loan,
                            currencySymbol = currencySymbol,
                            mainCurrencyCode = mainCurrencyCode,
                            onMarkAsRepaid = { viewModel.markAsRepaid(loan) },
                            onDelete = { deleteTargetLoan = loan }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLoanDialog(
            viewModel = viewModel,
            mainCurrencyCode = mainCurrencyCode,
            currencySymbol = currencySymbol,
            onDismiss = { showAddDialog = false },
            onConfirm = { loan ->
                viewModel.addLoan(loan)
                onFirstLoanCreated()
                showAddDialog = false
            }
        )
    }

    if (deleteTargetLoan != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetLoan = null },
            title = { Text(stringResource(R.string.debt_delete_confirm)) },
            text = {
                Text(stringResource(R.string.debt_delete_message_with_name, deleteTargetLoan!!.personName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLoan(deleteTargetLoan!!)
                        deleteTargetLoan = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetLoan = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun LoanItemCard(
    loan: Loan,
    currencySymbol: String,
    mainCurrencyCode: String,
    onMarkAsRepaid: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isLoanOut = loan.type == LOAN_TYPE_OUT
    val isOverdue = !loan.isRepaid && loan.dueDate != null && loan.dueDate < System.currentTimeMillis()
    val amountColor = if (isLoanOut) IncomeGreen else ExpenseRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLoanOut) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = loan.personName, fontSize = 18.sp)
                        Text(
                            text = DateUtils.formatDate(context, loan.date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        loan.dueDate?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.debt_due_date_value, DateUtils.formatDate(context, it)),
                                    fontSize = 12.sp,
                                    color = if (isOverdue) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (isOverdue) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.debt_overdue),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        if (loan.note.isNotBlank()) {
                            Text(
                                text = loan.note,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val hasOriginalCurrency = loan.originalCurrency != null &&
                        loan.originalCurrency != mainCurrencyCode &&
                        loan.originalAmount != null
                    Text(
                        text = "$currencySymbol${String.format("%.2f", loan.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = amountColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasOriginalCurrency) {
                        Text(
                            text = "(${getCurrencySymbol(loan.originalCurrency!!)}${String.format("%.2f", loan.originalAmount!!)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (loan.isRepaid) stringResource(R.string.debt_repaid) else stringResource(R.string.debt_unrepaid),
                        fontSize = 12.sp,
                        color = if (loan.isRepaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!loan.isRepaid) {
                    TextButton(onClick = onMarkAsRepaid) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = IncomeGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.debt_mark_repaid), color = IncomeGreen)
                    }
                }

                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}

@Composable
private fun AddLoanDialog(
    viewModel: ExpenseViewModel,
    mainCurrencyCode: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Loan) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(LOAN_TYPE_OUT) }
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(mainCurrencyCode) }
    var exchangeRate by remember { mutableStateOf<Double?>(null) }
    var isLoadingRate by remember { mutableStateOf(false) }
    var rateEditable by remember { mutableStateOf(false) }
    var rateInputText by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var pickingDueDate by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_add_record)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LOAN_TYPE_OUT,
                        onClick = { type = LOAN_TYPE_OUT },
                        label = { Text(stringResource(R.string.debt_lend)) }
                    )
                    FilterChip(
                        selected = type == LOAN_TYPE_IN,
                        onClick = { type = LOAN_TYPE_IN },
                        label = { Text(stringResource(R.string.debt_borrow)) }
                    )
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.debt_person_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.debt_amount_hint)) },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Text(code)
                        }
                    }
                }

                if (selectedCurrency != mainCurrencyCode) {
                    if (isLoadingRate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching rate...", fontSize = 12.sp)
                        }
                    } else {
                        val enteredAmount = amountText.toDoubleOrNull()
                        val effectiveRate = rateInputText.toDoubleOrNull() ?: exchangeRate
                        if (effectiveRate != null && enteredAmount != null) {
                            val convertedAmount = enteredAmount * effectiveRate
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "1 $selectedCurrency = ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.4f", effectiveRate),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { rateEditable = !rateEditable }
                                )
                                Text(
                                    text = " $mainCurrencyCode  →  $currencySymbol${String.format("%.2f", convertedAmount)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (rateEditable) {
                            OutlinedTextField(
                                value = rateInputText,
                                onValueChange = { rateInputText = it },
                                label = { Text("Exchange rate") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        pickingDueDate = false
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.debt_date_value, DateUtils.formatDate(context, date)))
                }

                OutlinedButton(
                    onClick = {
                        pickingDueDate = true
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dueDateLabel = dueDate?.let {
                        stringResource(R.string.debt_due_date_value, DateUtils.formatDate(context, it))
                    } ?: stringResource(R.string.debt_due_date_unset)
                    Text(dueDateLabel)
                }

                if (dueDate != null) {
                    TextButton(
                        onClick = { dueDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.debt_clear_due_date))
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.debt_note_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.debt_validation_error),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (personName.isBlank() || amount == null || amount <= 0) {
                        showError = true
                        return@TextButton
                    }

                    val isSameCurrency = selectedCurrency == mainCurrencyCode
                    val rateValue = if (isSameCurrency) null else rateInputText.toDoubleOrNull()
                    if (!isSameCurrency && rateValue == null) {
                        rateEditable = true
                        showError = true
                        return@TextButton
                    }

                    val convertedAmount = if (isSameCurrency) amount else amount * (rateValue ?: 1.0)

                    onConfirm(
                        Loan(
                            type = type,
                            personName = personName.trim(),
                            amount = convertedAmount,
                            date = date,
                            dueDate = dueDate,
                            note = note.trim(),
                            originalAmount = if (isSameCurrency) null else amount,
                            originalCurrency = if (isSameCurrency) null else selectedCurrency,
                            exchangeRate = if (isSameCurrency) null else rateValue
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

    if (showDatePicker) {
        if (pickingDueDate) {
            DueDatePickerDialog(
                initialDateTime = dueDate ?: date,
                onDismiss = { showDatePicker = false },
                onConfirm = { selectedTime ->
                    dueDate = selectedTime
                    showDatePicker = false
                }
            )
        } else {
            DateTimePickerDialog(
                initialDateTime = date,
                onDismiss = { showDatePicker = false },
                onConfirm = { selectedTime ->
                    date = selectedTime
                    showDatePicker = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDatePickerDialog(
    initialDateTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = initialDateTime
        }
    }

    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var showError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return utcTimeMillis >= today.timeInMillis
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_select_due_date)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.92f),
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextWeek = Calendar.getInstance().apply {
                                    add(Calendar.WEEK_OF_YEAR, 1)
                                }
                                datePickerState.selectedDateMillis = nextWeek.timeInMillis
                                selectedHour = nextWeek.get(Calendar.HOUR_OF_DAY)
                                selectedMinute = nextWeek.get(Calendar.MINUTE)
                                showError = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debt_next_week), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val nextMonth = Calendar.getInstance().apply {
                                    add(Calendar.MONTH, 1)
                                }
                                datePickerState.selectedDateMillis = nextMonth.timeInMillis
                                selectedHour = nextMonth.get(Calendar.HOUR_OF_DAY)
                                selectedMinute = nextMonth.get(Calendar.MINUTE)
                                showError = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debt_next_month), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DatePicker(state = datePickerState)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.record_time),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = String.format("%02d:%02d", selectedHour, selectedMinute),
                            fontSize = 36.sp,
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimePickerWheel(
                            items = (0..23).toList(),
                            selectedItem = selectedHour,
                            onItemSelected = {
                                selectedHour = it
                                showError = false
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        TimePickerWheel(
                            items = (0..59).toList(),
                            selectedItem = selectedMinute,
                            onItemSelected = {
                                selectedMinute = it
                                showError = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.debt_due_date_error),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { dateMillis ->
                                val finalCalendar = Calendar.getInstance().apply {
                                    timeInMillis = dateMillis
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }

                                val today = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }

                                if (finalCalendar.timeInMillis < today.timeInMillis) {
                                    showError = true
                                } else {
                                    onConfirm(finalCalendar.timeInMillis)
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
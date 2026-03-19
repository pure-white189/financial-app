package com.example.myapplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.data.Loan
import java.util.Calendar
import java.util.Locale

private const val LOAN_TYPE_IN = "借入"
private const val LOAN_TYPE_OUT = "借出"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPage(viewModel: ExpenseViewModel) {
    val loans by viewModel.loans.collectAsState(initial = emptyList())
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
        topBar = {
            TopAppBar(
                title = { Text("借贷管理") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加借贷")
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "待收总额", fontSize = 13.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "¥ %.2f", unrepaidOut),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "待还总额", fontSize = 13.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "¥ %.2f", unrepaidIn),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTabIndex) {
                listOf("全部", LOAN_TYPE_OUT, LOAN_TYPE_IN).forEachIndexed { index, title ->
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
                            text = "还没有借贷记录",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "点击右下角按钮添加第一条借贷记录",
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
                            Text("添加借贷")
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
            onDismiss = { showAddDialog = false },
            onConfirm = { loan ->
                viewModel.addLoan(loan)
                showAddDialog = false
            }
        )
    }

    if (deleteTargetLoan != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetLoan = null },
            title = { Text("删除借贷记录") },
            text = {
                Text("确定要删除与「${deleteTargetLoan!!.personName}」的借贷记录吗？此操作无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLoan(deleteTargetLoan!!)
                        deleteTargetLoan = null
                    }
                ) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetLoan = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun LoanItemCard(
    loan: Loan,
    onMarkAsRepaid: () -> Unit,
    onDelete: () -> Unit
) {
    val isLoanOut = loan.type == LOAN_TYPE_OUT
    val isOverdue = !loan.isRepaid && loan.dueDate != null && loan.dueDate < System.currentTimeMillis()
    val amountColor = if (isLoanOut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
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
                            text = DateUtils.formatDate(loan.date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        loan.dueDate?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "截止: ${DateUtils.formatDate(it)}",
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
                                        text = "已逾期",
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
                    Text(
                        text = String.format(Locale.getDefault(), "¥ %.2f", loan.amount),
                        fontSize = 20.sp,
                        color = amountColor
                    )
                    Text(
                        text = if (loan.isRepaid) "已还" else "未还",
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("标记已还")
                    }
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
private fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (Loan) -> Unit
) {
    var type by remember { mutableStateOf(LOAN_TYPE_OUT) }
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var pickingDueDate by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加借贷记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LOAN_TYPE_OUT,
                        onClick = { type = LOAN_TYPE_OUT },
                        label = { Text(LOAN_TYPE_OUT) }
                    )
                    FilterChip(
                        selected = type == LOAN_TYPE_IN,
                        onClick = { type = LOAN_TYPE_IN },
                        label = { Text(LOAN_TYPE_IN) }
                    )
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        showError = false
                    },
                    label = { Text("对方姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        showError = false
                    },
                    label = { Text("金额") },
                    prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                OutlinedButton(
                    onClick = {
                        pickingDueDate = false
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("日期：${DateUtils.formatDate(date)}")
                }

                OutlinedButton(
                    onClick = {
                        pickingDueDate = true
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dueDateLabel = dueDate?.let { "截止日期：${DateUtils.formatDate(it)}" } ?: "截止日期：未设置"
                    Text(dueDateLabel)
                }

                if (dueDate != null) {
                    TextButton(
                        onClick = { dueDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("清除截止日期")
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = "请填写对方姓名并输入大于 0 的金额",
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

                    onConfirm(
                        Loan(
                            type = type,
                            personName = personName.trim(),
                            amount = amount,
                            date = date,
                            dueDate = dueDate,
                            note = note.trim()
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
private fun DueDatePickerDialog(
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
        title = { Text("选择截止日期") },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        Text("下周", fontSize = 12.sp)
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
                        Text("下个月", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DatePicker(state = datePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "时间",
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
                        text = "截止日期不能早于今天",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.SavingGoal
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import java.util.Locale

private val GoalDoneColor = IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalPage(viewModel: ExpenseViewModel) {
    val goals by viewModel.savingGoals.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var depositTarget by remember { mutableStateOf<SavingGoal?>(null) }
    var deleteTarget by remember { mutableStateOf<SavingGoal?>(null) }
    var editTarget by remember { mutableStateOf<SavingGoal?>(null) }

    val ongoingCount = goals.count { !it.isCompleted }
    val totalSaved = goals.sumOf { it.currentAmount }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("储蓄目标") })
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
                    Icon(imageVector = Icons.Default.Add, contentDescription = "添加目标", tint = Color.White)
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
                        Text(text = "进行中目标数", fontSize = 13.sp)
                        Text(
                            text = ongoingCount.toString(),
                            fontSize = 24.sp,
                            color = PurpleStart
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "总已存金额", fontSize = 13.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "¥ %.2f", totalSaved),
                            fontSize = 20.sp,
                            color = IncomeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "还没有储蓄目标",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "点击右下角按钮添加你的第一个目标",
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
                            Text("添加目标")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        SavingGoalItemCard(
                            goal = goal,
                            onDeposit = { depositTarget = goal },
                            onEdit = { editTarget = goal },
                            onDelete = { deleteTarget = goal }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { goal ->
                viewModel.addGoal(goal)
                showAddDialog = false
            }
        )
    }

    depositTarget?.let { target ->
        DepositDialog(
            goal = target,
            onDismiss = { depositTarget = null },
            onConfirm = { amount ->
                viewModel.addDeposit(target, amount)
                depositTarget = null
            }
        )
    }

    editTarget?.let { target ->
        EditSavingGoalDialog(
            goal = target,
            onDismiss = { editTarget = null },
            onConfirm = { updatedGoal ->
                viewModel.updateGoal(updatedGoal)
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除储蓄目标") },
            text = { Text("确定要删除「${target.name}」吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(target)
                        deleteTarget = null
                    }
                ) {
                    Text(text = "删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SavingGoalItemCard(
    goal: SavingGoal,
    onDeposit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val percent = (progress * 100).toInt()
    val isOverdue = !goal.isCompleted && goal.deadline != null && goal.deadline < System.currentTimeMillis()

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
                    Text(text = goal.name, fontSize = 19.sp)
                    if (goal.note.isNotBlank()) {
                        Text(
                            text = goal.note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (goal.isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = IncomeGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = GoalDoneColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已完成",
                            fontSize = 12.sp,
                            color = GoalDoneColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "已存 %s / 目标 %s",
                    String.format(Locale.getDefault(), "¥ %.2f", goal.currentAmount),
                    String.format(Locale.getDefault(), "¥ %.2f", goal.targetAmount)
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (goal.isCompleted) GoalDoneColor else PurpleStart,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$percent%",
                fontSize = 12.sp,
                color = if (goal.isCompleted) GoalDoneColor else PurpleStart
            )

            goal.deadline?.let { deadline ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "截止：${DateUtils.formatDate(deadline)}",
                        fontSize = 12.sp,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!goal.isCompleted) {
                    TextButton(onClick = onDeposit) {
                        Text("存入")
                    }
                }

                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
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
private fun EditSavingGoalDialog(
    goal: SavingGoal,
    onDismiss: () -> Unit,
    onConfirm: (SavingGoal) -> Unit
) {
    var name by remember(goal.id) { mutableStateOf(goal.name) }
    var targetAmountText by remember(goal.id) { mutableStateOf(String.format(Locale.getDefault(), "%.2f", goal.targetAmount)) }
    var deadline by remember(goal.id) { mutableStateOf(goal.deadline) }
    var note by remember(goal.id) { mutableStateOf(goal.note) }
    var showError by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑目标") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("目标名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = {
                        targetAmountText = it
                        showError = false
                    },
                    label = { Text("目标金额") },
                    prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                OutlinedButton(
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val deadlineLabel = deadline?.let { "截止日期：${DateUtils.formatDate(it)}" } ?: "截止日期：未设置"
                    Text(deadlineLabel)
                }

                if (deadline != null) {
                    TextButton(
                        onClick = { deadline = null },
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
                        text = "请填写目标名称并输入大于 0 的目标金额",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newAmount = targetAmountText.toDoubleOrNull()
                    if (name.isBlank() || newAmount == null || newAmount <= 0) {
                        showError = true
                        return@TextButton
                    }

                    onConfirm(
                        goal.copy(
                            name = name.trim(),
                            targetAmount = newAmount,
                            deadline = deadline,
                            note = note.trim(),
                            isCompleted = goal.currentAmount >= newAmount
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

    if (showDueDatePicker) {
        DueDatePickerDialog(
            initialDateTime = deadline ?: System.currentTimeMillis(),
            onDismiss = { showDueDatePicker = false },
            onConfirm = { selectedTime ->
                deadline = selectedTime
                showDueDatePicker = false
            }
        )
    }
}

@Composable
private fun AddSavingGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (SavingGoal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加储蓄目标") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("目标名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = {
                        targetAmountText = it
                        showError = false
                    },
                    label = { Text("目标金额") },
                    prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )

                OutlinedButton(
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val deadlineLabel = deadline?.let { "截止日期：${DateUtils.formatDate(it)}" } ?: "截止日期：未设置"
                    Text(deadlineLabel)
                }

                if (deadline != null) {
                    TextButton(
                        onClick = { deadline = null },
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
                        text = "请填写目标名称并输入大于 0 的目标金额",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetAmount = targetAmountText.toDoubleOrNull()
                    if (name.isBlank() || targetAmount == null || targetAmount <= 0) {
                        showError = true
                        return@TextButton
                    }

                    onConfirm(
                        SavingGoal(
                            name = name.trim(),
                            targetAmount = targetAmount,
                            deadline = deadline,
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

    if (showDueDatePicker) {
        DueDatePickerDialog(
            initialDateTime = deadline ?: System.currentTimeMillis(),
            onDismiss = { showDueDatePicker = false },
            onConfirm = { selectedTime ->
                deadline = selectedTime
                showDueDatePicker = false
            }
        )
    }
}

@Composable
private fun DepositDialog(
    goal: SavingGoal,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    var amountText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存入金额") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "还需 ${String.format(Locale.getDefault(), "¥ %.2f", remaining)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100.0, 500.0, 1000.0).forEach { quickAmount ->
                        OutlinedButton(
                            onClick = {
                                amountText = String.format(Locale.getDefault(), "%.2f", quickAmount)
                                showError = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(quickAmount.toInt().toString(), fontSize = 12.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        amountText = String.format(Locale.getDefault(), "%.2f", remaining)
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("全部存入")
                }

                if (showError) {
                    Text(
                        text = "请输入大于 0 且不超过剩余金额的数值",
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
                    if (amount == null || amount <= 0 || amount > remaining) {
                        showError = true
                        return@TextButton
                    }
                    onConfirm(amount)
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


package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf(expense.note) }
    var selectedDate by remember { mutableStateOf(expense.date) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 初始化选中的类别
    LaunchedEffect(categories) {
        if (selectedCategory == null) {
            selectedCategory = categories.find { it.id == expense.categoryId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑消费记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 删除按钮
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
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
                label = { Text("金额") },
                placeholder = { Text("请输入金额") },
                leadingIcon = { Text("¥", fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                            text = "日期时间",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateUtils.formatDateForDisplay(selectedDate),
                            fontSize = 16.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "选择日期",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 类别选择
            Text(
                text = "类别",
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
                label = { Text("备注（可选）") },
                placeholder = { Text("添加备注说明") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && selectedCategory != null) {
                        val updatedExpense = expense.copy(
                            amount = amountValue,
                            categoryId = selectedCategory!!.id,
                            date = selectedDate,
                            note = note
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
                Text("保存修改", fontSize = 18.sp)
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
            title = { Text("确认删除") },
            text = { Text("确定要删除这条消费记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteExpense(expense)
                            onBack()
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseTemplate
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPage(
    viewModel: ExpenseViewModel,
    onNavigateToCategory: () -> Unit = {},
    alertThreshold: Double? = null
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<ExpenseTemplate?>(null) }
    var showAlertConfirm by remember { mutableStateOf(false) }
    var pendingExpense by remember { mutableStateOf<Expense?>(null) }

    val recentExpenses = remember(expenses) {
        expenses.take(5)
    }

    // 使用 Box 包裹，让 Snackbar 可以正确显示
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())  // 添加滚动
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "添加消费记录",
                    fontSize = 28.sp,
                    style = MaterialTheme.typography.headlineLarge
                )

                Row {
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "模板",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onNavigateToCategory) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "管理类别"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 快速模板 - 只显示置顶的
            val pinnedTemplates = remember(templates) {
                templates.filter { it.isPinned }
            }
            if (pinnedTemplates.isNotEmpty()) {
                Text(
                    text = "快速模板",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pinnedTemplates.forEach { template ->
                        val category = categories.find { it.id == template.categoryId }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.createExpenseFromTemplate(template)
                                    showSuccess = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category?.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = template.name,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "¥${template.amount}",
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedDate != System.currentTimeMillis()) {
                            TextButton(
                                onClick = { selectedDate = System.currentTimeMillis() }
                            ) {
                                Text("现在", fontSize = 12.sp)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择日期",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 类别选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择类别",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${categories.size} 个类别",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),  // 保持固定高度
                userScrollEnabled = true  // 禁用内部滚动，使用外部滚动
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

            // 按钮组
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 保存为模板
                OutlinedButton(
                    onClick = { showSaveTemplateDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = amount.toDoubleOrNull() != null &&
                            amount.toDoubleOrNull()!! > 0 &&
                            selectedCategory != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存模板")
                }

                // 添加记录
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null && amountValue > 0 && selectedCategory != null) {
                            val expense = Expense(
                                amount = amountValue,
                                categoryId = selectedCategory!!.id,
                                date = selectedDate,
                                note = note
                            )

                            // 检查是否超过提醒阈值
                            if (alertThreshold != null && amountValue >= alertThreshold) {
                                pendingExpense = expense
                                showAlertConfirm = true
                            } else {
                                // 直接添加
                                scope.launch {
                                    viewModel.addExpense(expense)
                                    amount = ""
                                    selectedCategory = null
                                    note = ""
                                    selectedDate = System.currentTimeMillis()
                                    showSuccess = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(2f),
                    enabled = amount.toDoubleOrNull() != null &&
                            amount.toDoubleOrNull()!! > 0 &&
                            selectedCategory != null
                ) {
                    Text("添加消费记录", fontSize = 16.sp)
                }
            }

            // 添加底部空白，确保按钮不会被底部导航遮挡
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 成功提示
        if (showSuccess) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showSuccess = false
            }

            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("添加成功！")
            }
        }
    }

    // 保存模板对话框
    if (showSaveTemplateDialog) {
        var templateName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("保存为模板") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("模板名称") },
                    placeholder = { Text("例如：每日交通") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (templateName.isNotBlank() && selectedCategory != null) {
                            val template = ExpenseTemplate(
                                name = templateName,
                                amount = amount.toDouble(),
                                categoryId = selectedCategory!!.id,
                                note = note
                            )
                            scope.launch {
                                viewModel.addTemplate(template)
                                showSaveTemplateDialog = false
                            }
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 模板和最近记录抽屉
    if (showTemplatesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatesSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)  // 限制最大高度
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "快速记账",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (templates.isNotEmpty()) {
                    Text(
                        text = "我的模板",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "长按模板可以置顶（最多3个），置顶的模板会显示在记账页面顶部",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    templates.forEach { template ->
                        TemplateItem(
                            template = template,
                            category = categories.find { it.id == template.categoryId },
                            onUse = {
                                scope.launch {
                                    viewModel.createExpenseFromTemplate(template)
                                    showTemplatesSheet = false
                                    showSuccess = true
                                }
                            },
                            onDelete = {
                                templateToDelete = template
                            },
                            onTogglePin = {
                                scope.launch {
                                    viewModel.toggleTemplatePinned(template)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (recentExpenses.isNotEmpty()) {
                    Text(
                        text = "最近记录（点击复制）",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    recentExpenses.forEach { expense ->
                        val category = categories.find { it.id == expense.categoryId }
                        RecentExpenseItem(
                            expense = expense,
                            category = category,
                            onCopy = {
                                amount = expense.amount.toString()
                                selectedCategory = category
                                note = expense.note
                                showTemplatesSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 删除模板确认对话框
    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("删除模板") },
            text = {
                Text("确定要删除「${templateToDelete?.name}」模板吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        templateToDelete?.let { template ->
                            scope.launch {
                                viewModel.deleteTemplate(template)
                                templateToDelete = null
                            }
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 大额消费确认对话框
    if (showAlertConfirm && pendingExpense != null) {
        AlertDialog(
            onDismissRequest = {
                showAlertConfirm = false
                pendingExpense = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("大额消费提醒") },
            text = {
                Column {
                    Text(
                        text = "本次消费金额较大，请确认是否继续",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = categories.find { it.id == pendingExpense!!.categoryId }?.name ?: "未知",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (pendingExpense!!.note.isNotEmpty()) {
                                    Text(
                                        text = pendingExpense!!.note,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = "¥${"%.2f".format(pendingExpense!!.amount)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.addExpense(pendingExpense!!)
                            amount = ""
                            selectedCategory = null
                            note = ""
                            selectedDate = System.currentTimeMillis()
                            showSuccess = true
                            showAlertConfirm = false
                            pendingExpense = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认记账")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAlertConfirm = false
                        pendingExpense = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateItem(
    template: ExpenseTemplate,
    category: Category?,
    onUse: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit  // 添加这个参数
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(  // 改用 combinedClickable 支持长按
                    onClick = { /* 不做任何事 */ },
                    onLongClick = onTogglePin
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 置顶标记
                if (template.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "已置顶",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = getCategoryIcon(category?.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template.name,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = "${category?.name} • ¥${template.amount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (template.note.isNotEmpty()) {
                        Text(
                            text = template.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Row {
                // 使用按钮
                IconButton(onClick = onUse) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "使用",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 删除按钮
                IconButton(onClick = onDelete) {
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
fun RecentExpenseItem(
    expense: Expense,
    category: Category?,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getCategoryIcon(category?.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = category?.name ?: "未知",
                        fontSize = 16.sp
                    )
                    Text(
                        text = DateUtils.formatDate(expense.date),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "¥${expense.amount}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
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
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                return utcTimeMillis <= today.timeInMillis
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期和时间") },
        properties = DialogProperties(
            usePlatformDefaultWidth = false  // 添加这行，不使用默认宽度
        ),
        modifier = Modifier
            .fillMaxWidth(0.95f)  // 添加这行，使用屏幕95%宽度
            .heightIn(max = 900.dp)  // 限制最大高度
            .wrapContentHeight(),  // 添加这行，高度自适应
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 快捷按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            datePickerState.selectedDateMillis = now.timeInMillis
                            selectedHour = now.get(Calendar.HOUR_OF_DAY)
                            selectedMinute = now.get(Calendar.MINUTE)
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("现在", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                            }
                            datePickerState.selectedDateMillis = today.timeInMillis
                            selectedHour = 12
                            selectedMinute = 0
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("今天", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val yesterday = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -1)
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                            }
                            datePickerState.selectedDateMillis = yesterday.timeInMillis
                            selectedHour = 12
                            selectedMinute = 0
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("昨天", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 日期选择器
                DatePicker(state = datePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                // 时间选择标题
                Text(
                    text = "时间",
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 显示当前选择的时间
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

                // iOS 风格的滚动选择器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小时选择器
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

                    // 分钟选择器
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

                Spacer(modifier = Modifier.height(16.dp))

                // 常用时间快捷按钮
                Text(
                    text = "常用时间",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

// 第一行：上午时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedHour = 0
                            selectedMinute = 0
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("凌晨", fontSize = 10.sp)
                            Text("00:00", fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedHour = 6
                            selectedMinute = 0
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("早晨", fontSize = 10.sp)
                            Text("06:00", fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

// 第二行：下午时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedHour = 12
                            selectedMinute = 0
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("中午", fontSize = 10.sp)
                            Text("12:00", fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedHour = 18
                            selectedMinute = 0
                            showError = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("傍晚", fontSize = 10.sp)
                            Text("18:00", fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                // 错误提示
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "不能选择未来的时间",
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

                        val now = System.currentTimeMillis()
                        if (finalCalendar.timeInMillis > now) {
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

@Composable
fun TimePickerWheel(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 初始化到选中位置
    LaunchedEffect(Unit) {
        val initialIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        listState.scrollToItem(initialIndex)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 选中指示器
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {}

        // 上下渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = 450f  // 固定高度值
                    )
                )
        )

        // 滚动列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 47.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .clickable {
                            onItemSelected(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", item),
                        fontSize = if (isSelected) 32.sp else 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }

    // 滚动停止时吸附
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress to listState.firstVisibleItemIndex
        }.collect { (isScrolling, _) ->
            if (!isScrolling) {
                delay(100)

                // 简单的逻辑：中心项就是 firstVisibleItemIndex
                val targetIndex = listState.firstVisibleItemIndex.coerceIn(items.indices)
                val targetValue = items[targetIndex]

                if (targetValue != selectedItem) {
                    onItemSelected(targetValue)
                }

                // 对齐
                coroutineScope.launch {
                    listState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = 0
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (category.iconPath != null) {
                AsyncImage(
                    model = category.iconPath,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                fontSize = 12.sp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
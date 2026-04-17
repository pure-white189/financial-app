package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.myapplication.data.Category
import com.example.myapplication.data.AiExpenseParser
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseTemplate
import com.example.myapplication.utils.displayName
import com.example.myapplication.utils.displayNote
import com.example.myapplication.ui.theme.IncomeGreen
import com.example.myapplication.ui.theme.PurpleEnd
import com.example.myapplication.ui.theme.PurpleStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPage(
    viewModel: ExpenseViewModel,
    onNavigateToCategory: () -> Unit = {},
    onBack: () -> Unit = {},
    alertThreshold: Double? = null,
    isGuest: Boolean = false,
    onNavigateToLogin: (() -> Unit)? = null
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val aiLang = context.getString(R.string.ai_prompt_language)
    val aiResultFilledText = stringResource(R.string.ai_result_filled)
    val aiParseFailedText = stringResource(R.string.ai_parse_failed)
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
    var aiInput by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiSuccess by remember { mutableStateOf<String?>(null) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var quotaDialogMessage by remember { mutableStateOf("") }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                    Text(
                        text = stringResource(R.string.record_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.template_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onNavigateToCategory) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.category_manage_title)
                        )
                    }
                }
            }


            // 快速模板 - 只显示置顶的
            val pinnedTemplates = remember(templates) {
                templates.filter { it.isPinned }
            }
            if (pinnedTemplates.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.template_title),
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
                                    text = template.displayName(category),
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

            // AI 自然语言输入区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                PurpleStart.copy(alpha = 0.1f),
                                PurpleEnd.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PurpleStart,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = aiInput,
                    onValueChange = { aiInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ai_input_hint),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (isGuest) {
                                onNavigateToLogin?.invoke()
                                return@KeyboardActions
                            }
                            if (aiInput.isNotBlank()) {
                                scope.launch {
                                    isAiLoading = true
                                    aiError = null
                                    aiSuccess = null
                                    val result = AiExpenseParser.parseExpense(aiInput, aiLang)
                                    result.onSuccess { parsed ->
                                        Log.d(
                                            "AiParser",
                                            "解析结果: amount=${parsed.amount} category=${parsed.category} note=${parsed.note}"
                                        )

                                        if (parsed.amount > 0) {
                                            amount = parsed.amount.toString()
                                        }

                                        val matchedCategory = categories.find {
                                            it.name.contains(parsed.category) ||
                                                parsed.category.contains(it.name)
                                        }
                                        if (matchedCategory != null) {
                                            selectedCategory = matchedCategory
                                        }

                                        if (parsed.note.isNotEmpty()) {
                                            note = parsed.note
                                        }

                                        aiSuccess = aiResultFilledText
                                        aiInput = ""
                                        delay(2000)
                                        aiSuccess = null
                                    }
                                    result.onFailure { error ->
                                        val message = error.message ?: ""
                                        val messageLower = message.lowercase()
                                        val isQuotaError = messageLower.contains("limit reached") ||
                                            messageLower.contains("upgrade to pro")
                                        val isNetworkFailure = messageLower.contains("failed to connect") ||
                                            messageLower.contains("timeout") ||
                                            messageLower.contains("unable to resolve host") ||
                                            messageLower.contains("connection")
                                        when {
                                            isQuotaError -> {
                                                quotaDialogMessage = message
                                                showQuotaDialog = true
                                            }
                                            isNetworkFailure -> aiError = aiParseFailedText
                                            else -> aiError = message
                                        }
                                    }
                                    isAiLoading = false
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleStart,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(PurpleStart, PurpleEnd)
                            )
                        )
                        .clickable {
                            if (isGuest) {
                                onNavigateToLogin?.invoke()
                                return@clickable
                            }
                            Log.d("AiParser", "发送按钮被点击，aiInput=$aiInput, isLoading=$isAiLoading")
                            scope.launch {
                                if (aiInput.isBlank() || isAiLoading) {
                                    return@launch
                                }
                                isAiLoading = true
                                aiError = null
                                aiSuccess = null
                                val result = AiExpenseParser.parseExpense(aiInput, aiLang)
                                result.onSuccess { parsed ->
                                    Log.d(
                                        "AiParser",
                                        "解析结果: amount=${parsed.amount} category=${parsed.category} note=${parsed.note}"
                                    )

                                    if (parsed.amount > 0) {
                                        amount = parsed.amount.toString()
                                    }

                                    val matchedCategory = categories.find {
                                        it.name.contains(parsed.category) ||
                                            parsed.category.contains(it.name)
                                    }
                                    if (matchedCategory != null) {
                                        selectedCategory = matchedCategory
                                    }

                                    if (parsed.note.isNotEmpty()) {
                                        note = parsed.note
                                    }

                                    aiSuccess = aiResultFilledText
                                    aiInput = ""
                                    delay(2000)
                                    aiSuccess = null
                                }
                                result.onFailure { error ->
                                    val message = error.message ?: ""
                                    val messageLower = message.lowercase()
                                    val isQuotaError = messageLower.contains("limit reached") ||
                                        messageLower.contains("upgrade to pro")
                                    val isNetworkFailure = messageLower.contains("failed to connect") ||
                                        messageLower.contains("timeout") ||
                                        messageLower.contains("unable to resolve host") ||
                                        messageLower.contains("connection")
                                    when {
                                        isQuotaError -> {
                                            quotaDialogMessage = message
                                            showQuotaDialog = true
                                        }
                                        isNetworkFailure -> aiError = aiParseFailedText
                                        else -> aiError = message
                                    }
                                }
                                isAiLoading = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = stringResource(R.string.analysis_ai_generate),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (aiError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = aiError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (isGuest) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_feature_locked),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (onNavigateToLogin != null) {
                        TextButton(onClick = onNavigateToLogin) {
                            Text(stringResource(R.string.ai_go_login))
                        }
                    }
                }
            }

            if (aiSuccess != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = aiSuccess!!,
                    color = IncomeGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 金额显示（由自定义键盘输入）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.debt_amount_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (amount.isEmpty()) "¥ 0" else "¥ $amount",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleStart
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 类别选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.record_select_category),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.record_category_count, categories.size),
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
                    .height(180.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // 紧凑日期选择行
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDateTimePicker = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = PurpleStart,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateUtils.formatDateForDisplay(context, selectedDate),
                            fontSize = 14.sp
                        )
                    }

                    if (selectedDate != System.currentTimeMillis()) {
                        TextButton(onClick = { selectedDate = System.currentTimeMillis() }) {
                            Text(stringResource(R.string.common_now), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 备注输入
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.record_note_hint)) },
                placeholder = { Text(stringResource(R.string.record_note_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            CalculatorKeyboard(
                onNumberClick = { digit ->
                    if (amount.length < 10) {
                        if (amount == "0") amount = digit
                        else amount += digit
                    }
                },
                onDeleteClick = {
                    if (amount.isNotEmpty()) {
                        amount = amount.dropLast(1)
                    }
                },
                onDotClick = {
                    if (!amount.contains(".")) {
                        if (amount.isEmpty()) amount = "0."
                        else amount += "."
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                    Text(stringResource(R.string.record_save_template))
                }

                // 添加记录
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = amount.toDoubleOrNull() != null &&
                                amount.toDoubleOrNull()!! > 0 &&
                                selectedCategory != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(stringResource(R.string.record_add_expense), fontSize = 16.sp, color = Color.White)
                    }
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
                Text(stringResource(R.string.record_add_success))
            }
        }
    }

    // 保存模板对话框
    if (showSaveTemplateDialog) {
        var templateName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text(stringResource(R.string.record_save_as_template_title)) },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text(stringResource(R.string.template_name_hint)) },
                    placeholder = { Text(stringResource(R.string.record_template_name_example)) },
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
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
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
                    text = stringResource(R.string.template_title),
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (templates.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.record_my_templates),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(R.string.record_templates_hint),
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
                        text = stringResource(R.string.record_recent_copy),
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
            title = { Text(stringResource(R.string.template_delete_confirm)) },
            text = {
                val category = categories.find { it.id == templateToDelete?.categoryId }
                Text(
                    stringResource(
                        R.string.record_delete_template_message,
                        templateToDelete?.displayName(category) ?: ""
                    )
                )
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
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
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
            title = { Text(stringResource(R.string.record_large_amount_confirm_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.record_large_amount_warning_desc),
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
                                    text = categories.find { it.id == pendingExpense!!.categoryId }?.name ?: stringResource(R.string.analysis_unknown_category),
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
                    Text(stringResource(R.string.record_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAlertConfirm = false
                        pendingExpense = null
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showQuotaDialog) {
        AlertDialog(
            onDismissRequest = { showQuotaDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.ai_quota_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (quotaDialogMessage.lowercase().contains("daily") ||
                            quotaDialogMessage.lowercase().contains("每日") ||
                            quotaDialogMessage.lowercase().contains("每日")
                        ) {
                            stringResource(R.string.ai_quota_parse_daily, 10)
                        } else {
                            stringResource(R.string.ai_quota_analyze_monthly, 2)
                        }
                    )
                    Text(
                        text = stringResource(R.string.ai_quota_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuotaDialog = false }) {
                    Text(stringResource(R.string.ai_quota_got_it))
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

@Composable
fun CalculatorKeyboard(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onDotClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (key == "⌫") {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable {
                                when (key) {
                                    "⌫" -> onDeleteClick()
                                    "." -> onDotClick()
                                    else -> onNumberClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "⌫") {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
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
                        contentDescription = stringResource(R.string.template_pin),
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
                            text = template.displayName(category),
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = "${category?.displayName() ?: stringResource(R.string.analysis_unknown_category)} • ¥${template.amount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (template.note.isNotEmpty()) {
                        Text(
                            text = template.displayNote(category),
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
                        contentDescription = stringResource(R.string.common_add),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 删除按钮
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
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
                        text = category?.displayName() ?: stringResource(R.string.analysis_unknown_category),
                        fontSize = 16.sp
                    )
                    Text(
                        text = DateUtils.formatDate(androidx.compose.ui.platform.LocalContext.current, expense.date),
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
        title = { Text(stringResource(R.string.record_date_time_picker_title)) },
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
                        Text(stringResource(R.string.common_now), fontSize = 12.sp)
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
                        Text(stringResource(R.string.home_today), fontSize = 12.sp)
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
                        Text(stringResource(R.string.record_yesterday), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 日期选择器
                DatePicker(state = datePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                // 时间选择标题
                Text(
                    text = stringResource(R.string.record_time),
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
                    text = stringResource(R.string.record_common_times),
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
                            Text(stringResource(R.string.record_time_midnight), fontSize = 10.sp)
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
                            Text(stringResource(R.string.record_time_morning), fontSize = 10.sp)
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
                            Text(stringResource(R.string.record_time_noon), fontSize = 10.sp)
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
                            Text(stringResource(R.string.record_time_evening), fontSize = 10.sp)
                            Text("18:00", fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                // 错误提示
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.record_future_time_error),
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
    val baseModifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)

    Box(
        modifier = if (isSelected) {
            baseModifier.background(
                brush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            baseModifier.background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
        }
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
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.displayName(),
                fontSize = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
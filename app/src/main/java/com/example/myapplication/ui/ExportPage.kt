package com.example.myapplication.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.myapplication.ExpenseViewModel
import com.example.myapplication.utils.CsvExportHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导出时间范围
 */
enum class ExportTimeRange(val label: String) {
    THIS_WEEK("本周"),
    THIS_MONTH("本月"),
    ALL("全部")
}

// 获取所有时间范围选项
val EXPORT_TIME_RANGE_OPTIONS = listOf(
    ExportTimeRange.THIS_WEEK,
    ExportTimeRange.THIS_MONTH,
    ExportTimeRange.ALL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPage(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 当前选择的时间范围
    var selectedTimeRange by remember { mutableStateOf(ExportTimeRange.THIS_MONTH) }

    // 导出预览数据
    var previewCount by remember { mutableStateOf(0) }
    var dateRangeText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 导出文件 URI
    var exportFileUri by remember { mutableStateOf<Uri?>(null) }

    // 显示导出成功对话框
    var showSuccessDialog by remember { mutableStateOf(false) }

    // 加载预览数据
    LaunchedEffect(selectedTimeRange) {
        val (start, end) = getTimeRange(selectedTimeRange)
        val expenses = viewModel.getExpensesByDateRange(start, end)
        previewCount = expenses.size
        dateRangeText = formatDateTime(start) + " ~ " + formatDateTime(end)
    }

    // 文件保存启动器（Storage Access Framework）
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            // 用户选择了保存位置，开始导出
            scope.launch {
                isLoading = true
                val (start, end) = getTimeRange(selectedTimeRange)
                val expenses = viewModel.getExpensesByDateRange(start, end)
                val categories = viewModel.getAllCategories().associateBy { it.id }

                // 生成 CSV 内容
                val csvContent = CsvExportHelper.generateCsvContent(expenses, categories)

                // 保存文件
                val result = CsvExportHelper.saveToFile(context, csvContent, fileUri)

                isLoading = false
                if (result.isSuccess) {
                    exportFileUri = fileUri
                    showSuccessDialog = true
                } else {
                    Toast.makeText(
                        context,
                        "导出失败：" + result.exceptionOrNull()?.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出数据") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 说明文字
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "导出数据为 CSV 格式，可用 Excel 或其他表格软件打开",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 时间范围选择
            Text(
                text = "选择时间范围",
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EXPORT_TIME_RANGE_OPTIONS.forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { selectedTimeRange = range },
                        label = { Text(range.label) },
                        leadingIcon = if (selectedTimeRange == range) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 导出预览
            Text(
                text = "导出预览",
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PreviewRow(
                        icon = Icons.Default.Receipt,
                        label = "预计导出记录数",
                        value = "$previewCount 条"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PreviewRow(
                        icon = Icons.Default.DateRange,
                        label = "时间范围",
                        value = dateRangeText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PreviewRow(
                        icon = Icons.Default.InsertDriveFile,
                        label = "文件格式",
                        value = "CSV (UTF-8)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 导出按钮
            Button(
                onClick = {
                    // 启动文件保存对话框
                    val fileName = CsvExportHelper.generateFileName()
                    saveFileLauncher.launch(fileName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = previewCount > 0 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在导出...")
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("导出到文件", fontSize = 18.sp)
                }
            }

            // 空状态提示
            if (previewCount == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前时间范围内没有消费记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 导出成功对话框
    if (showSuccessDialog && exportFileUri != null) {
        ExportSuccessDialog(
            fileUri = exportFileUri!!,
            onDismiss = {
                showSuccessDialog = false
                onBack()
            },
            onShare = { shareUri ->
                shareFile(context, shareUri)
            }
        )
    }
}

/**
 * 预览信息行组件
 */
@Composable
fun PreviewRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 导出成功对话框
 */
@Composable
fun ExportSuccessDialog(
    fileUri: Uri,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "导出成功",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "文件已保存到选择的位置",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 分享按钮
                OutlinedButton(
                    onClick = { onShare(fileUri) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分享")
                }

                // 完成按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("完成")
                }
            }
        }
    )
}

/**
 * 计算时间范围
 */
fun getTimeRange(range: ExportTimeRange): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    return when (range) {
        ExportTimeRange.THIS_WEEK -> {
            // 本周一 00:00
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            // 今天 23:59:59
            val end = System.currentTimeMillis()
            Pair(start, end)
        }
        ExportTimeRange.THIS_MONTH -> {
            // 本月 1 日 00:00
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            // 今天 23:59:59
            val end = System.currentTimeMillis()
            Pair(start, end)
        }
        ExportTimeRange.ALL -> {
            // 所有时间（从第一条记录到现在）
            Pair(0L, System.currentTimeMillis())
        }
    }
}

/**
 * 格式化日期时间
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 分享文件
 */
fun shareFile(context: Context, uri: Uri) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享导出文件"))
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "分享失败：" + e.message,
            Toast.LENGTH_SHORT
        ).show()
    }
}

package com.example.myapplication.utils

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV 导出工具类
 * 支持将消费记录导出为带 UTF-8 BOM 头的 CSV 文件，确保 Excel 打开不乱码
 */
object CsvExportHelper {

    // UTF-8 BOM 头 (EF BB BF)
    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    // CSV 表头
    private const val CSV_HEADER = "日期，类别，金额，备注"

    // 日期格式化
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 生成 CSV 内容
     * @param expenses 消费记录列表
     * @param categories 类别映射 (id -> Category)
     * @return CSV 格式字符串
     */
    fun generateCsvContent(
        expenses: List<Expense>,
        categories: Map<Int, Category>
    ): String {
        val sb = StringBuilder()

        // 添加表头
        sb.appendLine(CSV_HEADER)

        // 按时间倒序添加数据（最新的在前）
        expenses.sortedByDescending { it.date }.forEach { expense ->
            val category = categories[expense.categoryId]
            val categoryName = category?.name ?: "未知类别"

            // 格式化各字段
            val date = formatDate(expense.date)
            val amount = String.format(Locale.getDefault(), "%.2f", expense.amount)
            val note = escapeCsvField(expense.note)

            // 组合一行 CSV 数据
            sb.appendLine("$date,$categoryName,$amount,$note")
        }

        return sb.toString()
    }

    /**
     * CSV 字段转义
     * 如果字段包含逗号、双引号或换行符，需要用双引号包裹
     * 双引号需要转义为两个双引号
     */
    private fun escapeCsvField(field: String): String {
        if (field.isBlank()) return ""

        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            val escaped = field.replace("\"", "\"\"")
            "\"$escaped\""
        } else {
            field
        }
    }

    /**
     * 格式化日期时间戳
     */
    private fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 保存 CSV 内容到文件
     * @param context 上下文
     * @param csvContent CSV 内容
     * @param fileName 文件名（不含路径）
     * @param fileUri 文件 URI（由 SAF 获取）
     * @return 保存是否成功
     */
    suspend fun saveToFile(
        context: Context,
        csvContent: String,
        fileUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                // 写入 UTF-8 BOM 头
                outputStream.write(UTF8_BOM)

                // 写入 CSV 内容（使用 UTF-8 编码）
                outputStream.write(csvContent.toByteArray(StandardCharsets.UTF_8))

                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("无法打开文件输出流"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成默认文件名
     * 格式：财务管家_导出_YYYYMMDD_HHMMSS.csv
     */
    fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "财务管家_导出_${timestamp}.csv"
    }
}

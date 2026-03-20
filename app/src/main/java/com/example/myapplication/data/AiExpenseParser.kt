package com.example.myapplication.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AiExpenseParser {

    // 本地测试用 10.0.2.2，部署后改为云端地址
    private const val BASE_URL = "http://10.0.2.2:8000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class ParseResult(
        val amount: Double,
        val category: String,
        val note: String
    )

    suspend fun parseExpense(text: String): Result<ParseResult> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("AiParser", "开始请求，text=$text")  // 加这行
                val encodedText = URLEncoder.encode(text, "UTF-8")
                Log.d("AiParser", "请求URL: $BASE_URL/parse-expense?text=$encodedText")  // 加这行
                val request = Request.Builder()
                    .url("$BASE_URL/parse-expense?text=$encodedText")
                    .build()
                Log.d("AiParser", "发送请求中...")  // 加这行

                val response = client.newCall(request).execute()
                Log.d("AiParser", "收到响应: ${response.code}")  // 加这行
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("空响应"))

                val json = JSONObject(body)
                val result = ParseResult(
                    amount = json.optDouble("amount", 0.0),
                    category = json.optString("category", ""),
                    note = json.optString("note", "")
                )
                Result.success(result)
            } catch (e: Exception) {
                Log.e("AiParser", "请求异常: ${e.javaClass.simpleName}: ${e.message}")  // 加这行
                Result.failure(Exception(e.message ?: "未知错误"))
            }
        }
}


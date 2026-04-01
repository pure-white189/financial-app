package com.example.myapplication.data

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AiExpenseParser {

    // 本地测试用 10.0.2.2，部署后改为云端地址
    private const val BASE_URL = "http://20.199.169.108"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class ParseResult(
        val amount: Double,
        val category: String,
        val note: String
    )

    data class ExpenseSummary(
        val amount: Double,
        val category: String
    )

    data class StockPrice(
        val symbol: String,
        val price: Double,
        val currency: String
    )

    private fun requireIdToken(): Result<String> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(Exception("Please log in to use AI features"))

        return try {
            val tokenResult = Tasks.await(user.getIdToken(false))
            val token = tokenResult.token
            if (token.isNullOrBlank()) {
                Result.failure(Exception("Authentication failed. Please log in again."))
            } else {
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to get auth token"))
        }
    }

    private fun parseServerErrorMessage(body: String?, fallback: String): String {
        if (body.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(body)
            when {
                json.has("message") -> json.optString("message", fallback)
                json.has("detail") -> {
                    val detail = json.opt("detail")
                    when (detail) {
                        is String -> detail
                        is JSONObject -> detail.optString("message", fallback)
                        else -> fallback
                    }
                }
                json.has("error") -> json.optString("error", fallback)
                else -> fallback
            }
        } catch (_: Exception) {
            body
        }
    }

    suspend fun parseExpense(text: String): Result<ParseResult> =
        withContext(Dispatchers.IO) {
            try {
                val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
                Log.d("AiParser", "开始请求，text=$text")  // 加这行
                val encodedText = URLEncoder.encode(text, "UTF-8")
                Log.d("AiParser", "请求URL: $BASE_URL/parse-expense?text=$encodedText")  // 加这行
                val request = Request.Builder()
                    .url("$BASE_URL/parse-expense?text=$encodedText")
                    .header("Authorization", "Bearer $token")
                    .build()
                Log.d("AiParser", "发送请求中...")  // 加这行

                client.newCall(request).execute().use { response ->
                    Log.d("AiParser", "收到响应: ${response.code}")  // 加这行
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("空响应"))

                    if (!response.isSuccessful) {
                        val message = if (response.code == 429) {
                            parseServerErrorMessage(
                                body,
                                "Daily limit reached. Upgrade to Pro for unlimited access."
                            )
                        } else {
                            "HTTP ${response.code}: $body"
                        }
                        return@withContext Result.failure(Exception(message))
                    }

                    val json = JSONObject(body)
                    val result = ParseResult(
                        amount = json.optDouble("amount", 0.0),
                        category = json.optString("category", ""),
                        note = json.optString("note", "")
                    )
                    Result.success(result)
                }
            } catch (e: Exception) {
                Log.e("AiParser", "请求异常: ${e.javaClass.simpleName}: ${e.message}")  // 加这行
                Result.failure(Exception(e.message ?: "未知错误"))
            }
        }

    suspend fun analyzeExpenses(
        expenses: List<ExpenseSummary>,
        month: String = "本月"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
            Log.d("AiParser", "开始分析，共${expenses.size}条记录")

            val expenseJsonArray = JSONArray(
                expenses.map {
                    JSONObject().apply {
                        put("amount", it.amount)
                        put("category", it.category)
                    }
                }
            )

            val requestBody = JSONObject().apply {
                put("expenses", expenseJsonArray)
                put("month", month)
            }

            val body = requestBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/analyze-expenses")
                .post(body)
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("空响应"))

                if (!response.isSuccessful) {
                    val message = if (response.code == 429) {
                        parseServerErrorMessage(
                            responseBody,
                            "Daily limit reached. Upgrade to Pro for unlimited access."
                        )
                    } else {
                        "HTTP ${response.code}: $responseBody"
                    }
                    return@withContext Result.failure(
                        Exception(message)
                    )
                }

                val json = JSONObject(responseBody)
                val analysis = json.optString("analysis", "分析生成失败，请稍后重试")
                Result.success(analysis)
            }
        } catch (e: Exception) {
            Log.e("AiParser", "分析异常: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchStockPrices(symbols: List<String>): Result<Map<String, StockPrice>> =
        withContext(Dispatchers.IO) {
            try {
                if (symbols.isEmpty()) return@withContext Result.success(emptyMap())

                val symbolsParam = symbols.joinToString(",")
                val request = Request.Builder()
                    .url("$BASE_URL/stock-prices?symbols=$symbolsParam")
                    .build()

                Log.d("AiParser", "获取股票价格: $symbolsParam")

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("空响应"))

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                    }

                    Log.d("AiParser", "股票响应: $body")
                    val json = JSONObject(body)
                    val result = mutableMapOf<String, StockPrice>()

                    for (symbol in symbols) {
                        if (json.has(symbol)) {
                            val stockJson = json.getJSONObject(symbol)
                            if (!stockJson.has("error")) {
                                result[symbol] = StockPrice(
                                    symbol = symbol,
                                    price = stockJson.optDouble("price", 0.0),
                                    currency = stockJson.optString("currency", "HKD")
                                )
                            }
                        }
                    }
                    Result.success(result)
                }
            } catch (e: Exception) {
                Log.e("AiParser", "股票价格获取失败: ${e.message}")
                Result.failure(e)
            }
        }
}


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

    private const val BASE_URL = "http://20.199.169.108"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var localParseUsedToday: Int = 0
    private var localParseDate: String = ""
    private var localAnalyzeUsedThisMonth: Int = 0
    private var localAnalyzeMonth: String = ""
    private var localParseLimit: Int? = 10
    private var localAnalyzeLimit: Int? = 2
    private var localPlan: String = "free"

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

    data class SubscriptionStatus(
        val plan: String,
        val expiresAt: String?
    )

    data class UsageStatus(
        val plan: String,
        val parseUsed: Int,
        val parseLimit: Int?,
        val analyzeUsed: Int,
        val analyzeLimit: Int?
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

    /**
     * 解析自然语言记账输入。
     * @param text 用户输入的自然语言
     * @param lang AI 回复语言，取自 R.string.ai_prompt_language（"zh"/"en"/"zh-Hant"）
     */
    suspend fun parseExpense(text: String, lang: String = "zh"): Result<ParseResult> =
        withContext(Dispatchers.IO) {
            try {
                // Local quota pre-check (Plan B front-end guard)
                if (localPlan != "pro" && localParseLimit != null) {
                    val today = java.time.LocalDate.now().toString()
                    if (localParseDate == today && localParseUsedToday >= localParseLimit!!) {
                        return@withContext Result.failure(
                            Exception("Daily limit reached ($localParseLimit uses/day). Upgrade to Pro for unlimited access.")
                        )
                    }
                }

                val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
                Log.d("AiParser", "开始请求，text=$text, lang=$lang")
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val request = Request.Builder()
                    .url("$BASE_URL/parse-expense?text=$encodedText&lang=$lang")
                    .header("Authorization", "Bearer $token")
                    .build()
                Log.d("AiParser", "请求URL: $BASE_URL/parse-expense?text=$encodedText&lang=$lang")

                client.newCall(request).execute().use { response ->
                    Log.d("AiParser", "收到响应: ${response.code}")
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
                    Result.success(result).also {
                        val today = java.time.LocalDate.now().toString()
                        if (localParseDate != today) {
                            localParseDate = today
                            localParseUsedToday = 0
                        }
                        localParseUsedToday++
                    }
                }
            } catch (e: Exception) {
                Log.e("AiParser", "请求异常: ${e.javaClass.simpleName}: ${e.message}")
                Result.failure(Exception(e.message ?: "未知错误"))
            }
        }

    /**
     * 生成月度消费分析报告。
     * @param expenses 消费汇总列表
     * @param month 月份描述，如"本月"/"This Month"
     * @param lang AI 回复语言，取自 R.string.ai_prompt_language（"zh"/"en"/"zh-Hant"）
     */
    suspend fun analyzeExpenses(
        expenses: List<ExpenseSummary>,
        month: String = "本月",
        lang: String = "zh"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Local quota pre-check
            if (localPlan != "pro" && localAnalyzeLimit != null) {
                val month = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                if (localAnalyzeMonth == month && localAnalyzeUsedThisMonth >= localAnalyzeLimit!!) {
                    return@withContext Result.failure(
                        Exception("Monthly limit reached ($localAnalyzeLimit analyses/month). Upgrade to Pro for unlimited access.")
                    )
                }
            }

            val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
            Log.d("AiParser", "开始分析，共${expenses.size}条记录，lang=$lang")

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
                put("lang", lang)   // 传递语言给后端
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
                    return@withContext Result.failure(Exception(message))
                }

                val json = JSONObject(responseBody)
                val analysis = json.optString("analysis", "分析生成失败，请稍后重试")
                Result.success(analysis).also {
                    val month = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                    if (localAnalyzeMonth != month) {
                        localAnalyzeMonth = month
                        localAnalyzeUsedThisMonth = 0
                    }
                    localAnalyzeUsedThisMonth++
                }
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

    suspend fun fetchSubscriptionStatus(): Result<SubscriptionStatus> =
        withContext(Dispatchers.IO) {
            try {
                val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
                val request = Request.Builder()
                    .url("$BASE_URL/subscription-status")
                    .header("Authorization", "Bearer $token")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                    }
                    val json = JSONObject(body)
                    Result.success(
                        SubscriptionStatus(
                            plan = json.optString("plan", "free"),
                            expiresAt = if (json.isNull("expires_at")) null else json.optString("expires_at")
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun redeemCode(code: String): Result<SubscriptionStatus> =
        withContext(Dispatchers.IO) {
            try {
                val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
                val requestBody = JSONObject().apply { put("code", code.trim().uppercase()) }
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL/redeem-code")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    if (!response.isSuccessful) {
                        val message = parseServerErrorMessage(body, "Redemption failed")
                        return@withContext Result.failure(Exception(message))
                    }
                    val json = JSONObject(body)
                    Result.success(
                        SubscriptionStatus(
                            plan = json.optString("plan", "free"),
                            expiresAt = if (json.isNull("expires_at")) null else json.optString("expires_at")
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchUsageStatus(): Result<UsageStatus> =
        withContext(Dispatchers.IO) {
            try {
                val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
                val request = Request.Builder()
                    .url("$BASE_URL/usage-status")
                    .header("Authorization", "Bearer $token")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                    }
                    val json = JSONObject(body)
                    val parseObj = json.getJSONObject("parse")
                    val analyzeObj = json.getJSONObject("analyze")
                    val status = UsageStatus(
                        plan = json.optString("plan", "free"),
                        parseUsed = parseObj.optInt("used", 0),
                        parseLimit = if (parseObj.isNull("limit")) null else parseObj.optInt("limit"),
                        analyzeUsed = analyzeObj.optInt("used", 0),
                        analyzeLimit = if (analyzeObj.isNull("limit")) null else analyzeObj.optInt("limit")
                    )
                    // Sync local counters
                    val now = java.time.LocalDate.now()
                    localParseDate = now.toString()
                    localParseUsedToday = status.parseUsed
                    localParseLimit = status.parseLimit
                    val month = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                    localAnalyzeMonth = month
                    localAnalyzeUsedThisMonth = status.analyzeUsed
                    localAnalyzeLimit = status.analyzeLimit
                    localPlan = status.plan
                    Result.success(status)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
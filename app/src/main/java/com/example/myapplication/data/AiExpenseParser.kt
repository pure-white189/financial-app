package com.example.myapplication.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object AiExpenseParser {

    private const val BASE_URL = "http://20.199.169.108"
    private const val FREE_PARSE_DAILY_DEFAULT = 10
    private const val FREE_ANALYZE_MONTHLY_DEFAULT = 2

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

    private val _parseUsedToday = MutableStateFlow(0)
    val parseUsedToday: StateFlow<Int> = _parseUsedToday.asStateFlow()

    private val _parseLimit = MutableStateFlow<Int?>(10)
    val parseLimitFlow: StateFlow<Int?> = _parseLimit.asStateFlow()

    private val _planFlow = MutableStateFlow("free")
    val planFlow: StateFlow<String> = _planFlow.asStateFlow()

    data class ParseResult(
        val amount: Double,
        val category: String,
        val note: String,
        val date: String?,
        val time: String?,
        val currency: String?
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

    data class AnalyzeResult(
        val analysis: String,
        val recommendationType: String?,
        val recommendationStat: String?
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
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                        note = json.optString("note", ""),
                        date = json.optString("date", "").takeIf { it.isNotEmpty() && it != "null" },
                        time = json.optString("time", "").takeIf { it.isNotEmpty() && it != "null" },
                        currency = json.optString("currency", "").takeIf {
                            it.isNotEmpty() && it != "null" && it in listOf("HKD", "CNY", "USD")
                        }
                    )
                    Result.success(result).also {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        if (localParseDate != today) {
                            localParseDate = today
                            localParseUsedToday = 0
                        }
                        localParseUsedToday++
                        _parseUsedToday.value = localParseUsedToday
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
                val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
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
                val result = AnalyzeResult(
                    analysis = json.optString("analysis", "分析生成失败，请稍后重试"),
                    recommendationType = if (json.isNull("recommendation_type")) null else json.optString("recommendation_type", "").takeIf { it.isNotEmpty() },
                    recommendationStat = if (json.isNull("recommendation_stat")) null else json.optString("recommendation_stat", "").takeIf { it.isNotEmpty() }
                )
                Result.success(result.analysis).also {
                    val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
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

    suspend fun analyzeExpensesDetailed(
        expenses: List<ExpenseSummary>,
        month: String = "本月",
        lang: String = "zh"
    ): Result<AnalyzeResult> = withContext(Dispatchers.IO) {
        try {
            // Local quota pre-check
            if (localPlan != "pro" && localAnalyzeLimit != null) {
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                if (localAnalyzeMonth == currentMonth && localAnalyzeUsedThisMonth >= localAnalyzeLimit!!) {
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
                put("lang", lang)
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
                val result = AnalyzeResult(
                    analysis = json.optString("analysis", "分析生成失败，请稍后重试"),
                    recommendationType = if (json.isNull("recommendation_type")) null else json.optString("recommendation_type", "").takeIf { it.isNotEmpty() },
                    recommendationStat = if (json.isNull("recommendation_stat")) null else json.optString("recommendation_stat", "").takeIf { it.isNotEmpty() }
                )

                Result.success(result).also {
                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                    if (localAnalyzeMonth != currentMonth) {
                        localAnalyzeMonth = currentMonth
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

    suspend fun fetchExchangeRate(from: String, to: String): Double? {
        android.util.Log.d("ExchangeRate", "fetchExchangeRate called: $from -> $to")
        return withContext(Dispatchers.IO) {
            try {
                val encodedFrom = URLEncoder.encode(from, "UTF-8")
                val encodedTo = URLEncoder.encode(to, "UTF-8")
                val request = Request.Builder()
                    .url("$BASE_URL/exchange-rate?from_currency=$encodedFrom&to_currency=$encodedTo")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val json = response.body?.string() ?: return@withContext null
                    android.util.Log.d("ExchangeRate", "response: $json")
                    val obj = JSONObject(json)
                    if (obj.has("error")) null else obj.getDouble("rate")
                }
            } catch (e: Exception) {
                android.util.Log.e("ExchangeRate", "error: ${e::class.simpleName} ${e.message}")
                null
            }
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

    suspend fun redeemTokensForAi(type: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val token = requireIdToken().getOrElse { return@withContext Result.failure(it) }
                val requestBody = JSONObject().apply { put("type", type) }
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL/redeem-tokens")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful) {
                        if (type == "parse") {
                            localParseUsedToday = (localParseLimit ?: 10) - 1
                            _parseUsedToday.value = localParseUsedToday
                        } else if (type == "analyze") {
                            localAnalyzeUsedThisMonth = (localAnalyzeLimit ?: 2) - 1
                        }
                        val newBalance = try {
                            JSONObject(body ?: "").optInt("new_balance", 0)
                        } catch (_: Exception) { 0 }
                        return@withContext Result.success(newBalance)
                    }
                    val message = parseServerErrorMessage(body, "Redemption failed")
                    Result.failure(Exception(message))
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
                    val now = Date()
                    localParseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                    localParseUsedToday = status.parseUsed
                    _parseUsedToday.value = localParseUsedToday
                    localParseLimit = status.parseLimit
                    _parseLimit.value = localParseLimit
                    val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now)
                    localAnalyzeMonth = month
                    localAnalyzeUsedThisMonth = status.analyzeUsed
                    localAnalyzeLimit = status.analyzeLimit
                    localPlan = status.plan
                    _planFlow.value = localPlan
                    Result.success(status)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchRecommendations(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/recommendations")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadRecommendations(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val cachedVersion = prefs[ThemePreferences.RECOMMENDATIONS_VERSION_KEY] ?: 0
        val cachedJson = prefs[ThemePreferences.RECOMMENDATIONS_CACHE_KEY]

        val freshJson = fetchRecommendations() ?: return@withContext cachedJson

        return@withContext try {
            val fetchedVersion = JSONObject(freshJson).optInt("version", 0)
            if (fetchedVersion >= cachedVersion) {
                context.dataStore.edit { preferences ->
                    preferences[ThemePreferences.RECOMMENDATIONS_CACHE_KEY] = freshJson
                    preferences[ThemePreferences.RECOMMENDATIONS_VERSION_KEY] = fetchedVersion
                }
            }
            freshJson
        } catch (_: Exception) {
            cachedJson
        }
    }

    suspend fun fetchTokenBalance(): Int = withContext(Dispatchers.IO) {
        try {
            val token = requireIdToken().getOrElse { return@withContext 0 }
            val request = Request.Builder()
                .url("$BASE_URL/token-balance")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext 0
                val body = response.body?.string() ?: return@withContext 0
                val json = JSONObject(body)
                json.optInt("balance", 0)
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun performCheckIn(): CheckInResult = withContext(Dispatchers.IO) {
        val token = requireIdToken().getOrElse {
            return@withContext CheckInResult.NetworkError("Not logged in")
        }
        try {
            val request = Request.Builder()
                .url("$BASE_URL/check-in")
                .addHeader("Authorization", "Bearer $token")
                .post(ByteArray(0).toRequestBody(null))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                    ?: return@withContext CheckInResult.NetworkError("Empty response")
                if (!response.isSuccessful) {
                    return@withContext CheckInResult.NetworkError("Server error ${response.code}")
                }
                val json = JSONObject(body)
                CheckInResult.Success(
                    alreadyCheckedIn = json.optBoolean("already_checked_in", false),
                    streak = json.optInt("streak", 1),
                    baseTokens = json.optInt("base_tokens", 0),
                    bonusTokens = json.optInt("bonus_tokens", 0),
                    newBalance = json.optInt("new_balance", 0)
                )
            }
        } catch (e: Exception) {
            CheckInResult.NetworkError(e.message ?: "Unknown error")
        }
    }

    suspend fun performUnlockAchievement(achievementId: String): AchievementResult =
        withContext(Dispatchers.IO) {
            val token = requireIdToken().getOrElse {
                return@withContext AchievementResult.NetworkError("Not logged in")
            }
            try {
                val bodyJson = JSONObject().put("achievement_id", achievementId).toString()
                val request = Request.Builder()
                    .url("$BASE_URL/unlock-achievement")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext AchievementResult.NetworkError("Empty response")
                    if (!response.isSuccessful) {
                        return@withContext AchievementResult.NetworkError("Server error ${response.code}")
                    }
                    val json = JSONObject(body)
                    AchievementResult.Success(
                        alreadyUnlocked = json.optBoolean("already_unlocked", false),
                        achievementId = json.optString("achievement_id", achievementId),
                        tokensEarned = json.optInt("tokens_earned", 0),
                        newBalance = json.optInt("new_balance", 0)
                    )
                }
            } catch (e: Exception) {
                AchievementResult.NetworkError(e.message ?: "Unknown error")
            }
        }

    suspend fun fetchCheckInStatus(): CheckInStatusResult = withContext(Dispatchers.IO) {
        val token = requireIdToken().getOrElse {
            return@withContext CheckInStatusResult.NetworkError("Not logged in")
        }
        try {
            val request = Request.Builder()
                .url("$BASE_URL/check-in-status")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                    ?: return@withContext CheckInStatusResult.NetworkError("Empty response")
                if (!response.isSuccessful) {
                    return@withContext CheckInStatusResult.NetworkError("Server error ${response.code}")
                }
                val json = JSONObject(body)
                CheckInStatusResult.Success(
                    alreadyCheckedIn = json.optBoolean("already_checked_in", false),
                    streak = json.optInt("streak", 0),
                    balance = json.optInt("balance", 0)
                )
            }
        } catch (e: Exception) {
            CheckInStatusResult.NetworkError(e.message ?: "Unknown error")
        }
    }
}

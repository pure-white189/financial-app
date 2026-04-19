package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CheckInRepository(
    private val checkInDao: CheckInDao,
    private val tokenTransactionDao: TokenTransactionDao
) {

    companion object {
        // Streak milestone bonus tokens (on top of base +1)
        val STREAK_BONUSES = mapOf(
            7 to 5,
            30 to 15,
            90 to 30,
            365 to 100,
            730 to 200
        )

        // Achievement definitions: achievementId -> token reward
        val ACHIEVEMENT_TOKENS = mapOf(
            // Behavior achievements
            "first_expense"      to 5,
            "set_budget"         to 5,
            "set_saving_goal"    to 5,
            "first_loan"         to 5,
            "first_sync"         to 10,
            "first_ai_parse"     to 10,
            "first_ai_analyze"   to 10,
            "first_stock"        to 5,
            "first_income"       to 5,
            // Streak achievements (check-in days)
            "streak_7"           to 5,
            "streak_30"          to 15,
            "streak_90"          to 30,
            "streak_365"         to 100,
            "streak_730"         to 200,
            // Budget achievements (consecutive months under budget)
            "budget_1"           to 10,
            "budget_3"           to 20,
            "budget_6"           to 40,
            "budget_9"           to 60,
            "budget_12"          to 100,
            "budget_18"          to 150,
            "budget_24"          to 200
        )

        private const val DATE_PATTERN = "yyyy-MM-dd"

        fun todayString(): String = SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date())
    }

    // ── Flows ──────────────────────────────────────────────────────────────

    fun getAllCheckIns(): Flow<List<CheckIn>> = checkInDao.getAllCheckIns()
    fun getTokenBalance(): Flow<Int> = tokenTransactionDao.getTokenBalance()
    fun getAllTransactions(): Flow<List<TokenTransaction>> = tokenTransactionDao.getAllTransactions()

    // ── Check-in ───────────────────────────────────────────────────────────

    /**
     * Attempt today's check-in.
     * Returns CheckInResult describing what happened.
     */
    suspend fun checkIn(): CheckInResult {
        val today = todayString()

        // Already checked in today
        if (checkInDao.getCheckInByDate(today) != null) {
            return CheckInResult.AlreadyDone
        }

        // Calculate current streak (how many consecutive days before today)
        val streak = calculateStreakBefore(today) + 1  // +1 for today

        // Base reward
        var tokensEarned = 1
        val bonusReason = STREAK_BONUSES[streak]
        if (bonusReason != null) tokensEarned += bonusReason

        val now = System.currentTimeMillis()

        // Save check-in record
        checkInDao.insertCheckIn(
            CheckIn(
                date = today,
                tokensEarned = tokensEarned,
                updatedAt = now
            )
        )

        // Save token transaction
        tokenTransactionDao.insertTransaction(
            TokenTransaction(
                type = "checkin",
                amount = tokensEarned,
                description = "checkin_$today",
                timestamp = now,
                updatedAt = now
            )
        )

        // Check if streak milestone achievement should unlock
        val streakAchievementId = when (streak) {
            7 -> "streak_7"
            30 -> "streak_30"
            90 -> "streak_90"
            365 -> "streak_365"
            730 -> "streak_730"
            else -> null
        }

        return CheckInResult.Success(
            tokensEarned = tokensEarned,
            currentStreak = streak,
            streakAchievementId = streakAchievementId
        )
    }

    /**
     * Calculate consecutive check-in streak ending on the day before [todayStr].
     */
    private suspend fun calculateStreakBefore(todayStr: String): Int {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
        var streak = 0
        val calendar = Calendar.getInstance().apply {
            time = sdf.parse(todayStr) ?: Date()
            add(Calendar.DAY_OF_YEAR, -1)
        }
        while (true) {
            val record = checkInDao.getCheckInByDate(sdf.format(calendar.time))
            if (record != null && record.isDeleted == 0) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    /**
     * Calculate current streak including today if checked in.
     */
    suspend fun getCurrentStreak(): Int {
        val today = todayString()
        val todayRecord = checkInDao.getCheckInByDate(today)
        val base = if (todayRecord != null && todayRecord.isDeleted == 0) {
            calculateStreakBefore(today) + 1
        } else {
            calculateStreakBefore(today)
        }
        return base
    }

    suspend fun hasCheckedInToday(): Boolean {
        return checkInDao.getCheckInByDate(todayString()) != null
    }

    // ── Achievements ───────────────────────────────────────────────────────

    /**
     * Unlock an achievement if not already unlocked.
     * Awards tokens automatically.
     * Returns true if newly unlocked.
     */
    suspend fun unlockAchievement(achievementDao: AchievementDao, achievementId: String): Boolean {
        val existing = achievementDao.getAchievementById(achievementId)
        if (existing != null && existing.unlockedAt > 0L) return false   // already unlocked

        val tokens = ACHIEVEMENT_TOKENS[achievementId] ?: 0
        val now = System.currentTimeMillis()

        achievementDao.insertAchievement(
            Achievement(
                achievementId = achievementId,
                unlockedAt = now,
                tokensAwarded = tokens,
                updatedAt = now
            )
        )

        if (tokens > 0) {
            tokenTransactionDao.insertTransaction(
                TokenTransaction(
                    type = "achievement",
                    amount = tokens,
                    description = achievementId,
                    timestamp = now,
                    updatedAt = now
                )
            )
        }

        return true
    }

    // ── Token redemption ───────────────────────────────────────────────────

    sealed class RedeemResult {
        data class Success(val remaining: Int) : RedeemResult()
        data class InsufficientTokens(val balance: Int, val required: Int) : RedeemResult()
    }

    /**
     * Spend tokens for AI features.
     * type: "parse" costs 5, "analyze" costs 15
     */
    suspend fun redeemTokens(type: String): RedeemResult {
        val cost = if (type == "analyze") 15 else 5
        val balance = tokenTransactionDao.getTokenBalanceOnce()
        if (balance < cost) return RedeemResult.InsufficientTokens(balance, cost)

        val now = System.currentTimeMillis()
        tokenTransactionDao.insertTransaction(
            TokenTransaction(
                type = "redeem",
                amount = -cost,
                description = "redeem_$type",
                timestamp = now,
                updatedAt = now
            )
        )
        return RedeemResult.Success(balance - cost)
    }

    suspend fun redeemTokensAndNotifyBackend(type: String, aiParser: AiExpenseParser): RedeemResult {
        val localResult = redeemTokens(type)
        if (localResult is RedeemResult.InsufficientTokens) return localResult

        val backendResult = aiParser.redeemTokensForAi(type)
        if (backendResult.isFailure) {
            val cost = if (type == "analyze") 15 else 5
            val now = System.currentTimeMillis()
            tokenTransactionDao.insertTransaction(
                TokenTransaction(
                    type = "redeem_rollback",
                    amount = cost,
                    description = "rollback_$type",
                    timestamp = now,
                    updatedAt = now
                )
            )
            return RedeemResult.InsufficientTokens(balance = 0, required = cost)
        }

        return localResult
    }
}

// ── Result types ───────────────────────────────────────────────────────────

sealed class CheckInResult {
    object AlreadyDone : CheckInResult()
    data class Success(
        val tokensEarned: Int,
        val currentStreak: Int,
        val streakAchievementId: String?   // non-null if milestone reached
    ) : CheckInResult()
}


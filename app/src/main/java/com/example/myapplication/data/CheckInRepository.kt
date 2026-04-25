package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CheckInRepository(
    private val checkInDao: CheckInDao,
    private val tokenTransactionDao: TokenTransactionDao,
    private val achievementDao: AchievementDao,
    val aiExpenseParser: AiExpenseParser
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
    fun getAllTransactions(): Flow<List<TokenTransaction>> = tokenTransactionDao.getAllTransactions()

    // ── Check-in ───────────────────────────────────────────────────────────

    /**
     * Attempt today's check-in.
     * Returns CheckInResult describing what happened.
     */
    suspend fun checkIn(): CheckInResult {
        val result = aiExpenseParser.performCheckIn()
        if (result is CheckInResult.Success && !result.alreadyCheckedIn) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val now = System.currentTimeMillis()

            // Cache locally for display and Firestore sync.
            val checkIn = CheckIn(
                date = today,
                tokensEarned = result.baseTokens + result.bonusTokens,
                updatedAt = now
            )
            checkInDao.insertCheckIn(checkIn)

            // Update local token transaction log for history display.
            if (result.baseTokens > 0) {
                tokenTransactionDao.insertTransaction(
                    TokenTransaction(
                        type = "checkin",
                        amount = result.baseTokens,
                        description = "daily_check_in",
                        timestamp = now,
                        updatedAt = now
                    )
                )
            }
            if (result.bonusTokens > 0) {
                tokenTransactionDao.insertTransaction(
                    TokenTransaction(
                        type = "checkin",
                        amount = result.bonusTokens,
                        description = "streak_bonus_${result.streak}",
                        timestamp = now,
                        updatedAt = now
                    )
                )
            }
        }
        return result
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
    suspend fun unlockAchievement(achievementId: String): AchievementResult {
        val result = aiExpenseParser.performUnlockAchievement(achievementId)
        if (result is AchievementResult.Success && !result.alreadyUnlocked) {
            val now = System.currentTimeMillis()

            // Cache locally.
            achievementDao.insertAchievement(
                Achievement(
                    achievementId = achievementId,
                    unlockedAt = now,
                    tokensAwarded = result.tokensEarned,
                    updatedAt = now
                )
            )

            if (result.tokensEarned > 0) {
                tokenTransactionDao.insertTransaction(
                    TokenTransaction(
                        type = "achievement",
                        amount = result.tokensEarned,
                        description = "achievement_$achievementId",
                        timestamp = now,
                        updatedAt = now
                    )
                )
            }
        }
        return result
    }

    suspend fun getTokenBalance(): Int {
        return aiExpenseParser.fetchTokenBalance()
    }

    // ── Token redemption ───────────────────────────────────────────────────

    sealed class RedeemResult {
        data class Success(val newBalance: Int) : RedeemResult()
        object Failure : RedeemResult()
    }

    suspend fun redeemTokensAndNotifyBackend(type: String, aiParser: AiExpenseParser): RedeemResult {
        val result = aiParser.redeemTokensForAi(type)
        return if (result.isSuccess) {
            val newBalance = result.getOrDefault(0)
            RedeemResult.Success(newBalance)
        } else {
            RedeemResult.Failure
        }
    }
}


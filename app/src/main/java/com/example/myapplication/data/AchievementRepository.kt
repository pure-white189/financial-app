package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val checkInRepository: CheckInRepository
) {

    companion object {
        // All achievement IDs grouped by category
        val BEHAVIOR_ACHIEVEMENTS = listOf(
            "first_expense", "set_budget", "set_saving_goal", "first_loan",
            "first_sync", "first_ai_parse", "first_ai_analyze", "first_stock",
            "first_income"
        )
        val STREAK_ACHIEVEMENTS = listOf(
            "streak_7", "streak_30", "streak_90", "streak_365", "streak_730"
        )
        val BUDGET_ACHIEVEMENTS = listOf(
            "budget_1", "budget_3", "budget_6", "budget_9",
            "budget_12", "budget_18", "budget_24"
        )
        // Maps budget achievement ID -> required consecutive months
        val BUDGET_MILESTONES = mapOf(
            "budget_1" to 1, "budget_3" to 3, "budget_6" to 6,
            "budget_9" to 9, "budget_12" to 12, "budget_18" to 18, "budget_24" to 24
        )
    }

    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    /**
     * Check budget achievements on app startup.
     * Looks at monthly expenses vs budget across past months.
     * [monthlyTotals] is a map of "yyyy-MM" -> total spending for that month.
     * [monthlyBudget] is the current budget setting (applied uniformly).
     * Returns list of newly unlocked achievement IDs.
     */
    suspend fun checkBudgetAchievements(
        monthlyTotals: Map<String, Double>,
        monthlyBudget: Double,
        monthlyIncomeDao: MonthlyIncomeDao
    ): List<String> {
        if (monthlyBudget <= 0.0) return emptyList()

        // Build sorted list of past months (exclude current month, most recent first)
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val sortedMonths = monthlyTotals.keys
            .filter { it != currentMonth }
            .sortedDescending()

        // Count consecutive months under budget from most recent going back
        var consecutive = 0
        for (month in sortedMonths) {
            val spending = monthlyTotals[month] ?: 0.0
            if (spending <= monthlyBudget) {
                consecutive++
            } else {
                break
            }
        }

        // Unlock any newly reached milestones
        val newlyUnlocked = mutableListOf<String>()
        for ((achievementId, required) in BUDGET_MILESTONES) {
            if (consecutive >= required) {
                val result = checkInRepository.unlockAchievement(achievementId)
                if (result is AchievementResult.Success && !result.alreadyUnlocked) {
                    newlyUnlocked.add(achievementId)
                }
            }
        }
        return newlyUnlocked
    }

    /**
     * Unlock a behavior achievement. Returns true if newly unlocked.
     */
    suspend fun unlockBehaviorAchievement(achievementId: String): Boolean {
        val result = checkInRepository.unlockAchievement(achievementId)
        return result is AchievementResult.Success && !result.alreadyUnlocked
    }

    /**
     * Check if an achievement is already unlocked.
     */
    suspend fun isUnlocked(achievementId: String): Boolean {
        val a = achievementDao.getAchievementById(achievementId)
        return a != null && a.unlockedAt > 0L
    }
}


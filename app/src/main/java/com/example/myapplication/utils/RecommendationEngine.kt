package com.example.myapplication.utils

import com.example.myapplication.data.Expense

data class RecommendationTrigger(
    val trigger: String,
    val stat: String
)

object RecommendationEngine {

    fun evaluate(
        expenses: List<Expense>,
        monthlyBudget: Float,
        currentMonthSpend: Float,
        stockCount: Int,
        streakDays: Int,
        totalExpenseCount: Int,
        currencySymbol: String,
        categoryKeyMap: Map<Long, String>
    ): RecommendationTrigger? {

        // 1. over_budget: budget set and usage > 80%
        if (monthlyBudget > 0 && currentMonthSpend / monthlyBudget > 0.8f) {
            val pct = (currentMonthSpend / monthlyBudget * 100).toInt()
            return RecommendationTrigger("over_budget", "$pct%")
        }

        // 2. food_heavy: food category > 35% of total spend
        val total = expenses.sumOf { it.amount }.toFloat()
        if (total > 0) {
            val foodTotal = expenses.filter { categoryKeyMap[it.categoryId.toLong()] == "food" }.sumOf { it.amount }.toFloat()
            val shoppingTotal = expenses.filter { categoryKeyMap[it.categoryId.toLong()] == "shopping" }.sumOf { it.amount }.toFloat()
            val entertainmentTotal = expenses.filter { categoryKeyMap[it.categoryId.toLong()] == "entertainment" }.sumOf { it.amount }.toFloat()

            if (foodTotal / total > 0.35f)
                return RecommendationTrigger("food_heavy", "${(foodTotal / total * 100).toInt()}%")
            if (shoppingTotal / total > 0.30f)
                return RecommendationTrigger("shopaholic", "${(shoppingTotal / total * 100).toInt()}%")
            if (entertainmentTotal / total > 0.25f)
                return RecommendationTrigger("entertainment_heavy", "${(entertainmentTotal / total * 100).toInt()}%")
            val medicalTotal = expenses.filter { categoryKeyMap[it.categoryId.toLong()] == "medical" }.sumOf { it.amount }.toFloat()
            if (medicalTotal / total > 0.25f)
                return RecommendationTrigger("medical_heavy", "${(medicalTotal / total * 100).toInt()}%")
        }

        // 3. investor: has stocks
        if (stockCount > 0)
            return RecommendationTrigger("investor", "$stockCount")

        // 4. streak_user: streak >= 7 days
        if (streakDays >= 7)
            return RecommendationTrigger("streak_user", "$streakDays")

        // 5. new_user: fewer than 10 total records
        if (totalExpenseCount < 10)
            return RecommendationTrigger("new_user", "")

        return null
    }
}


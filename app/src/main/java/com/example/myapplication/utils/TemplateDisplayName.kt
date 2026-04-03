package com.example.myapplication.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.data.Category
import com.example.myapplication.data.ExpenseTemplate

private const val EPSILON = 0.0001

private enum class BuiltInTemplateKey {
    COMMUTE,
    LUNCH,
    COFFEE,
    MOVIE
}

private fun Double.near(other: Double): Boolean = kotlin.math.abs(this - other) < EPSILON

private fun ExpenseTemplate.builtInKey(category: Category?): BuiltInTemplateKey? {
    val categoryKey = category?.categoryKey

    val normalizedName = name.trim().lowercase()
    val normalizedNote = note.trim().lowercase()

    return when {
        categoryKey == "transport" && amount.near(10.0) &&
            (normalizedName in setOf("commute", "车费", "每日交通", "每日通勤") ||
                normalizedNote in setOf("subway/bus", "地铁/公交")) -> BuiltInTemplateKey.COMMUTE

        categoryKey == "food" && amount.near(20.0) &&
            (normalizedName in setOf("lunch", "午餐") ||
                normalizedNote in setOf("weekday lunch", "工作日午餐")) -> BuiltInTemplateKey.LUNCH

        categoryKey == "food" && amount.near(15.0) &&
            (normalizedName in setOf("coffee", "咖啡") ||
                normalizedNote in setOf("morning coffee", "早晨咖啡")) -> BuiltInTemplateKey.COFFEE

        categoryKey == "entertainment" && amount.near(50.0) &&
            (normalizedName in setOf("movie", "看电影", "看電影") ||
                normalizedNote in setOf("movie ticket", "电影票", "電影票")) -> BuiltInTemplateKey.MOVIE

        else -> null
    }
}

@Composable
fun ExpenseTemplate.displayName(category: Category?): String {
    return when (builtInKey(category)) {
        BuiltInTemplateKey.COMMUTE -> stringResource(R.string.template_default_commute_name)
        BuiltInTemplateKey.LUNCH -> stringResource(R.string.template_default_lunch_name)
        BuiltInTemplateKey.COFFEE -> stringResource(R.string.template_default_coffee_name)
        BuiltInTemplateKey.MOVIE -> stringResource(R.string.template_default_movie_name)
        null -> name
    }
}

@Composable
fun ExpenseTemplate.displayNote(category: Category?): String {
    return when (builtInKey(category)) {
        BuiltInTemplateKey.COMMUTE -> stringResource(R.string.template_default_commute_note)
        BuiltInTemplateKey.LUNCH -> stringResource(R.string.template_default_lunch_note)
        BuiltInTemplateKey.COFFEE -> stringResource(R.string.template_default_coffee_note)
        BuiltInTemplateKey.MOVIE -> stringResource(R.string.template_default_movie_note)
        null -> note
    }
}

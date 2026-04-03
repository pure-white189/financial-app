package com.example.myapplication

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    // 格式化日期用于显示
    fun formatDateForDisplay(context: Context, timestamp: Long): String {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return when {
            isSameDay(today, selectedCal) -> {
                "${context.getString(R.string.date_today)} ${formatWithSkeleton(timestamp, "jm")}" 
            }
            isYesterday(today, selectedCal) -> {
                "${context.getString(R.string.date_yesterday)} ${formatWithSkeleton(timestamp, "jm")}" 
            }
            isThisYear(selectedCal) -> {
                formatWithSkeleton(timestamp, "MMMdjm")
            }
            else -> {
                formatWithSkeleton(timestamp, "yMMMdjm")
            }
        }
    }

    // 格式化日期（简短版本，用于列表）
    fun formatDate(context: Context, timestamp: Long): String {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return when {
            isSameDay(today, selectedCal) -> {
                "${context.getString(R.string.date_today)} ${formatWithSkeleton(timestamp, "jm")}" 
            }
            isYesterday(today, selectedCal) -> {
                "${context.getString(R.string.date_yesterday)} ${formatWithSkeleton(timestamp, "jm")}" 
            }
            isThisYear(selectedCal) -> {
                formatWithSkeleton(timestamp, "MMMdjm")
            }
            else -> {
                formatWithSkeleton(timestamp, "yMMMdjm")
            }
        }
    }

    // Use locale skeletons so date order/separators follow current app language.
    private fun formatWithSkeleton(timestamp: Long, skeleton: String): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return SimpleDateFormat(pattern, locale).format(Date(timestamp))
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, date)
    }

    private fun isThisYear(date: Calendar): Boolean {
        val today = Calendar.getInstance()
        return date.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }
}
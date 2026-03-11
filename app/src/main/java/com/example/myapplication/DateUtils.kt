package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    // 格式化日期用于显示
    fun formatDateForDisplay(timestamp: Long): String {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return when {
            isSameDay(today, selectedCal) -> {
                "今天 ${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))}"
            }
            isYesterday(today, selectedCal) -> {
                "昨天 ${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))}"
            }
            isThisYear(selectedCal) -> {
                SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(Date(timestamp))
            }
        }
    }

    // 格式化日期（简短版本，用于列表）
    fun formatDate(timestamp: Long): String {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return when {
            isSameDay(today, selectedCal) -> {
                "今天 ${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))}"
            }
            isYesterday(today, selectedCal) -> {
                "昨天 ${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))}"
            }
            isThisYear(selectedCal) -> {
                SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(Date(timestamp))
            }
        }
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
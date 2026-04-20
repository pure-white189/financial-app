package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.SyncRepository

class FinanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ExpenseRepository(
            categoryDao = database.categoryDao(),
            expenseDao = database.expenseDao(),
            expenseTemplateDao = database.expenseTemplateDao(),
            loanDao = database.loanDao(),
            savingGoalDao = database.savingGoalDao(),
            stockDao = database.stockDao(),
            monthlyIncomeDao = database.monthlyIncomeDao(),
            checkInDao = database.checkInDao(),
            achievementDao = database.achievementDao(),
            tokenTransactionDao = database.tokenTransactionDao(),
            aiReportDao = database.aiReportDao()
        )
    }
    val syncRepository by lazy {
        SyncRepository(
            database.expenseDao(),
            database.loanDao(),
            database.savingGoalDao(),
            database.stockDao(),
            database.monthlyIncomeDao(),
            database.checkInDao(),
            database.achievementDao(),
            database.tokenTransactionDao(),
            database.aiReportDao()
        )
    }
}
package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.SyncRepository

class FinanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ExpenseRepository(
            database.categoryDao(),
            database.expenseDao(),
            database.expenseTemplateDao(),
            database.loanDao(),
            database.savingGoalDao(),
            database.stockDao()
        )
    }
    val syncRepository by lazy {
        SyncRepository(
            database.expenseDao(),
            database.loanDao(),
            database.savingGoalDao(),
            database.stockDao(),
            database.monthlyIncomeDao()
        )
    }
}
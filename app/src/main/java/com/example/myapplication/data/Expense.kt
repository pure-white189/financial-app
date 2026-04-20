package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val categoryId: Int,
    val date: Long,
    val note: String = "",
    val originalAmount: Double? = null,
    val originalCurrency: String? = null,
    val exchangeRate: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
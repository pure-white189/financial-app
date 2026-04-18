package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_income")
data class MonthlyIncome(
    @PrimaryKey val yearMonth: String, // format: "2026-04"
    val amount: Double,
    val note: String = "",
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)


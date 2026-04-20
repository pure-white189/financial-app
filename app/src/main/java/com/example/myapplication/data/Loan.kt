package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val personName: String,
    val amount: Double,
    val date: Long,
    val dueDate: Long? = null,
    val note: String = "",
    val originalAmount: Double? = null,
    val originalCurrency: String? = null,
    val exchangeRate: Double? = null,
    val isRepaid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_templates")
data class ExpenseTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double,
    val categoryId: Int,
    val note: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val categoryKey: String = ""
)
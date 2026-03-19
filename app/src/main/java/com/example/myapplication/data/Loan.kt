package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,           // "借入" 或 "借出"
    val personName: String,
    val amount: Double,
    val date: Long,
    val dueDate: Long? = null,
    val note: String = "",
    val isRepaid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

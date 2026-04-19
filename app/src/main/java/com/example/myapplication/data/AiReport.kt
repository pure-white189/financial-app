package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_reports")
data class AiReport(
    @PrimaryKey val yearMonth: String,
    val content: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0
)


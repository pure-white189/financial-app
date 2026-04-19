package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_reports")
data class AiReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val content: String,
    val generatedAt: String,
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0
)


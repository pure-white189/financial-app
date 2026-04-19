package com.example.myapplication.data

import androidx.room.*

@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey val date: String,        // format: "2026-04-18"
    val tokensEarned: Int = 0,
    val firestoreId: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Int = 0
)


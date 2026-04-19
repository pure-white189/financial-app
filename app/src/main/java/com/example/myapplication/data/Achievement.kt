package com.example.myapplication.data

import androidx.room.*

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val achievementId: String,  // e.g. "first_expense", "streak_7"
    val unlockedAt: Long = 0L,              // epoch millis, 0 = not unlocked
    val tokensAwarded: Int = 0,
    val firestoreId: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Int = 0
)


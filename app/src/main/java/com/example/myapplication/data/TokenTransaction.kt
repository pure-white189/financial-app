package com.example.myapplication.data

import androidx.room.*

@Entity(tableName = "token_transactions")
data class TokenTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,           // "checkin" / "achievement" / "redeem"
    val amount: Int,            // positive = earned, negative = spent
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val firestoreId: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Int = 0
)


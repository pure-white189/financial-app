package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val symbol: String,
    val name: String,
    val shares: Double = 0.0,
    val costPrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val lastUpdated: Long = 0L,
    val market: String = "HK",
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)


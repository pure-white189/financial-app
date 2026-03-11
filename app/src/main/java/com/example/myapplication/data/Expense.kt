package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,           // 金额
    val categoryId: Int,          // 所属类别 ID
    val date: Long,               // 日期时间戳
    val note: String = "",        // 备注
    val createdAt: Long = System.currentTimeMillis() // 创建时间
)
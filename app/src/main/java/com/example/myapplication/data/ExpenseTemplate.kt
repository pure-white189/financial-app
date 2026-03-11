package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_templates")
data class ExpenseTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,              // 模板名称，如 "每日交通"
    val amount: Double,            // 金额
    val categoryId: Int,           // 类别 ID
    val note: String = "",         // 备注
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
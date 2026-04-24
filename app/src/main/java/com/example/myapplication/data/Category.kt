package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val categoryKey: String = "",
    val iconPath: String? = null,
    val iconName: String? = null,
    val color: String,
    val isDefault: Boolean = false,
    val firestoreId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

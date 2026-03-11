package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,              // 类别名称，如 "餐饮"
    val iconPath: String? = null,  // 用户上传的图片路径（可选）
    val iconName: String? = null,  // 预设图标名称（可选）
    val color: String,             // 颜色值，如 "#FF5722"
    val isDefault: Boolean = false // 是否是预设类别
)
package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,              // 自定义类别存名称；默认类别存 key（如 "food"），仅作备用
    val categoryKey: String = "",  // 默认类别的 key，自定义类别为空字符串
    val iconPath: String? = null,  // 用户上传的图片路径（可选）
    val iconName: String? = null,  // 预设图标名称（可选）
    val color: String,             // 颜色值，如 "#FF5722"
    val isDefault: Boolean = false // 是否是预设类别
)

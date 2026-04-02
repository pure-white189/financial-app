package com.example.myapplication.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.data.Category

/**
 * 根据 categoryKey 从当前语言的 strings.xml 取显示名称。
 * - 默认类别（categoryKey 非空）：从 strings.xml 取，随语言切换联动
 * - 自定义类别（categoryKey 为空）：直接显示 name 字段
 *
 * 用法：将所有 `category.name` 替换为 `category.displayName()`
 *
 * 示例：
 *   Text(category.displayName())
 *   Text(text = category.displayName())
 */
@Composable
fun Category.displayName(): String {
    if (categoryKey.isEmpty()) return name  // 自定义类别

    val resId = categoryKeyToResId(categoryKey) ?: return name
    return stringResource(resId)
}

/**
 * categoryKey → string resource ID 的映射表。
 * 如果将来新增默认类别，在此处补充即可。
 */
fun categoryKeyToResId(key: String): Int? = when (key) {
    "food"          -> R.string.category_food
    "transport"     -> R.string.category_transport
    "shopping"      -> R.string.category_shopping
    "entertainment" -> R.string.category_entertainment
    "health"        -> R.string.category_health
    "education"     -> R.string.category_education
    "housing"       -> R.string.category_housing
    "other"         -> R.string.category_other
    else            -> null
}

/**
 * 非 Composable 环境（如 CSV 导出、通知文字）使用此方法。
 * 需要传入 Context 手动取字符串。
 */
fun Category.displayNameNonComposable(context: android.content.Context): String {
    if (categoryKey.isEmpty()) return name
    val resId = categoryKeyToResId(categoryKey) ?: return name
    return context.getString(resId)
}

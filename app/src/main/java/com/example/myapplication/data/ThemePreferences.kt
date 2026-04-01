package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 主题模式枚举
enum class ThemeMode {
    LIGHT,   // 浅色
    DARK,    // 深色
    SYSTEM   // 跟随系统
}

// DataStore 扩展
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class ThemePreferences(private val context: Context) {

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val MONTHLY_BUDGET_KEY = stringPreferencesKey("monthly_budget")
        val EXPENSE_ALERT_THRESHOLD_KEY = stringPreferencesKey("expense_alert_threshold")
        val SHOW_PERSISTENT_NOTIFICATION_KEY = booleanPreferencesKey("show_persistent_notification")  // 添加这行
        val REQUIRE_DELETE_CONFIRM_KEY = booleanPreferencesKey("require_delete_confirm")
        val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        val AUTO_BACKUP_KEY = booleanPreferencesKey("auto_backup")
    }

    // 读取主题设置
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[THEME_MODE_KEY]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM  // 默认跟随系统
            }
        }

    // 读取月度预算
    val monthlyBudget: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[MONTHLY_BUDGET_KEY]?.toDoubleOrNull()
        }

    // 读取消费提醒阈值
    val expenseAlertThreshold: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[EXPENSE_ALERT_THRESHOLD_KEY]?.toDoubleOrNull()
        }

    // 读取常驻通知开关
    val showPersistentNotification: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_PERSISTENT_NOTIFICATION_KEY] ?: false
        }

    val requireDeleteConfirm: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REQUIRE_DELETE_CONFIRM_KEY] ?: true
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_KEY] ?: false
    }

    // 保存常驻通知开关
    suspend fun setShowPersistentNotification(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PERSISTENT_NOTIFICATION_KEY] = show
        }
    }

    suspend fun setRequireDeleteConfirm(require: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REQUIRE_DELETE_CONFIRM_KEY] = require
        }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = seen
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_KEY] = enabled
        }
    }

    // 保存消费提醒阈值
    suspend fun setExpenseAlertThreshold(threshold: Double?) {
        context.dataStore.edit { preferences ->
            if (threshold == null) {
                preferences.remove(EXPENSE_ALERT_THRESHOLD_KEY)
            } else {
                preferences[EXPENSE_ALERT_THRESHOLD_KEY] = threshold.toString()
            }
        }
    }

    // 保存月度预算
    suspend fun setMonthlyBudget(budget: Double?) {
        context.dataStore.edit { preferences ->
            if (budget == null) {
                preferences.remove(MONTHLY_BUDGET_KEY)
            } else {
                preferences[MONTHLY_BUDGET_KEY] = budget.toString()
            }
        }
    }

    // 保存主题设置
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
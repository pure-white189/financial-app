package com.example.myapplication.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.myapplication.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object LanguageManager {

    private const val TAG = "LangDebug"

    enum class AppLanguage(val tag: String, val displayName: String) {
        FOLLOW_SYSTEM("", "跟随系统 / Follow System / 跟隨系統"),
        ZH_HANS("zh-Hans", "简体中文"),
        ZH_HANT("zh-Hant", "繁體中文"),
        EN("en", "English");

        companion object {
            fun fromTag(tag: String): AppLanguage =
                entries.find { it.tag == tag } ?: FOLLOW_SYSTEM
        }
    }

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")
    private val HAS_CHOSEN_LANGUAGE_KEY = booleanPreferencesKey("has_chosen_language")

    // ===== Flow（给 Compose collectAsState 用）=====

    fun languageFlow(context: Context): Flow<AppLanguage> =
        context.dataStore.data.map { prefs ->
            AppLanguage.fromTag(prefs[LANGUAGE_KEY] ?: "")
        }

    fun hasChosenLanguageFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[HAS_CHOSEN_LANGUAGE_KEY] ?: false
        }

    // ===== 写入（suspend，给协程调用）=====

    suspend fun saveLanguage(context: Context, language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.tag
        }
        applyLanguage(language)
    }

    suspend fun markLanguageChosen(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[HAS_CHOSEN_LANGUAGE_KEY] = true
        }
    }

    // ===== 应用语言到系统 =====

    fun applyLanguage(language: AppLanguage) {
        Log.d(TAG, "Applying language: ${language.tag.ifBlank { "FOLLOW_SYSTEM" }}")
        val localeList = if (language.tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    // ===== 关键：同步读取并应用，在 onCreate setContent 之前调用 =====
    // 用 runBlocking 确保语言在 setContent 渲染前就已经应用
    fun restoreLanguageOnStartup(context: Context) {
        val tag = runBlocking {
            context.dataStore.data.first()[LANGUAGE_KEY] ?: ""
        }
        val language = AppLanguage.fromTag(tag)
        Log.d(TAG, "Restoring saved language tag: ${tag.ifBlank { "FOLLOW_SYSTEM" }}")
        applyLanguage(language)
    }
}
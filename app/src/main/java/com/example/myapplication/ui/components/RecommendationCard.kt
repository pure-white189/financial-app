package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecommendationCard(
    recommendationsJson: String,
    trigger: String,
    stat: String,
    lang: String,
    onMapsSearch: (String) -> Unit,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToStocks: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val item = remember(recommendationsJson, trigger) {
        try {
            val root = org.json.JSONObject(recommendationsJson)
            val items = root.getJSONArray("items")
            var found: org.json.JSONObject? = null
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                if (obj.getString("trigger") == trigger) {
                    found = obj
                    break
                }
            }
            found
        } catch (_: Exception) { null }
    } ?: return

    val langKey = when (lang) {
        "en" -> "en"
        "zh-Hant" -> "zh-Hant"
        else -> "zh"
    }

    fun localizedStr(obj: org.json.JSONObject, key: String): String =
        try { obj.getJSONObject(key).optString(langKey, "") } catch (_: Exception) { "" }

    val title = localizedStr(item, "title").replace("{stat}", stat)
    val subtitle = localizedStr(item, "subtitle").replace("{stat}", stat)
    val ctaLabel = localizedStr(item, "cta_label")
    val ctaType = item.optString("type", "article")
    val ctaQuery = try { item.getJSONObject("cta_query").optString(langKey, "") } catch (_: Exception) { "" }
    val ctaDestination = item.optString("cta_destination", "")

    // article body
    val articleKey = item.optString("article_key", "")
    val articleBody = remember(recommendationsJson, articleKey, langKey) {
        if (articleKey.isEmpty()) return@remember ""
        try {
            val root = org.json.JSONObject(recommendationsJson)
            root.getJSONObject("articles").getJSONObject(articleKey).optString(langKey, "")
        } catch (_: Exception) { "" }
    }
    var showArticle by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)

            if (showArticle && articleBody.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(articleBody, fontSize = 13.sp, lineHeight = 20.sp)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    when (ctaType) {
                        "maps_search" -> onMapsSearch(ctaQuery)
                        "in_app" -> {
                            when (ctaDestination) {
                                "check_in" -> onNavigateToCheckIn()
                                "saving_goals", "savings" -> onNavigateToSavings()
                                "stocks", "stock" -> onNavigateToStocks()
                                "settings" -> onNavigateToSettings()
                            }
                        }
                        "article" -> showArticle = !showArticle
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (ctaType == "article" && showArticle) "收起" else ctaLabel, fontSize = 13.sp)
            }
        }
    }
}


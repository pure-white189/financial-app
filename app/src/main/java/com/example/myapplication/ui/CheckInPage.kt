package com.example.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.Achievement
import com.example.myapplication.data.AchievementRepository
import com.example.myapplication.data.CheckInResult
import com.example.myapplication.data.TokenTransaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInPage(
    viewModel: CheckInViewModel,
    onBack: () -> Unit
) {
    val tokenBalance by viewModel.tokenBalance.collectAsState()
    val alreadyCheckedInToday by viewModel.alreadyCheckedInToday.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val allAchievements by viewModel.allAchievements.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val checkInResult by viewModel.checkInResult.collectAsState()
    val checkInMessage by viewModel.checkInMessage.collectAsState()

    // Snackbar for check-in result
    val snackbarHostState = remember { SnackbarHostState() }
    val networkErrorText = stringResource(R.string.network_error_check_in)
    val alreadyCheckedInText = stringResource(R.string.check_in_already)

    // Token history collapsed by default
    var historyExpanded by remember { mutableStateOf(false) }

    // Show snackbar when check-in result arrives
    val streakBonusMsg = stringResource(R.string.checkin_streak_bonus)
    val tokensEarnedMsg = stringResource(R.string.checkin_tokens_earned)
    LaunchedEffect(checkInResult) {
        val result = checkInResult ?: return@LaunchedEffect
        if (result is CheckInResult.Success && !result.alreadyCheckedIn) {
            val tokensEarned = result.baseTokens + result.bonusTokens
            val hasStreakBonus = result.bonusTokens > 0
            val msg = if (hasStreakBonus) {
                streakBonusMsg.format(result.streak, tokensEarned)
            } else {
                tokensEarnedMsg.format(tokensEarned)
            }
            snackbarHostState.showSnackbar(msg)
        }
        viewModel.consumeCheckInResult()
    }

    LaunchedEffect(checkInMessage) {
        when (checkInMessage) {
            "network_error" -> snackbarHostState.showSnackbar(networkErrorText)
            "already_checked_in" -> snackbarHostState.showSnackbar(alreadyCheckedInText)
            else -> Unit
        }
        if (checkInMessage != null) viewModel.clearCheckInMessage()
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.checkin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Check-in card ──────────────────────────────────────────
            item {
                CheckInCard(
                    streak = currentStreak,
                    tokenBalance = tokenBalance,
                    alreadyCheckedInToday = alreadyCheckedInToday,
                    onCheckIn = { viewModel.checkIn() }
                )
            }

            // ── Achievements: Behavior ─────────────────────────────────
            item {
                AchievementSectionHeader(stringResource(R.string.achievements_behavior))
            }
            items(AchievementRepository.BEHAVIOR_ACHIEVEMENTS) { id ->
                val achievement = allAchievements.find { it.achievementId == id }
                AchievementRow(achievementId = id, achievement = achievement)
            }

            // ── Achievements: Streak ───────────────────────────────────
            item {
                AchievementSectionHeader(stringResource(R.string.achievements_streak))
            }
            items(AchievementRepository.STREAK_ACHIEVEMENTS) { id ->
                val achievement = allAchievements.find { it.achievementId == id }
                AchievementRow(achievementId = id, achievement = achievement)
            }

            // ── Achievements: Budget ───────────────────────────────────
            item {
                AchievementSectionHeader(stringResource(R.string.achievements_budget))
            }
            items(AchievementRepository.BUDGET_ACHIEVEMENTS) { id ->
                val achievement = allAchievements.find { it.achievementId == id }
                AchievementRow(achievementId = id, achievement = achievement)
            }

            // ── Token history (collapsible) ────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.token_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { historyExpanded = !historyExpanded }) {
                        Text(if (historyExpanded) "▲" else "▼")
                    }
                }
            }
            if (historyExpanded) {
                items(allTransactions.take(30)) { tx ->
                    TokenTransactionRow(tx)
                }
            }
        }
    }
}

// ── Check-in card ──────────────────────────────────────────────────────────

@Composable
private fun CheckInCard(
    streak: Int,
    tokenBalance: Int,
    alreadyCheckedInToday: Boolean,
    onCheckIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔥", fontSize = 28.sp)
                Text(
                    text = stringResource(R.string.checkin_streak, streak),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Token balance
            Text(
                text = stringResource(R.string.token_balance, tokenBalance),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Check-in button
            Button(
                onClick = { if (!alreadyCheckedInToday) onCheckIn() },
                enabled = !alreadyCheckedInToday,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (alreadyCheckedInToday) Icons.Filled.CheckCircle else Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (alreadyCheckedInToday) {
                        stringResource(R.string.check_in_done)
                    } else {
                        stringResource(R.string.check_in_today)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Achievement section header ─────────────────────────────────────────────

@Composable
private fun AchievementSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

// ── Achievement row ────────────────────────────────────────────────────────

@Composable
private fun AchievementRow(
    achievementId: String,
    achievement: Achievement?
) {
    val isUnlocked = achievement != null && achievement.unlockedAt > 0L
    val tokens = com.example.myapplication.data.CheckInRepository.ACHIEVEMENT_TOKENS[achievementId] ?: 0

    val nameRes = achievementNameRes(achievementId)
    val descRes = achievementDescRes(achievementId)

    val containerAlpha = if (isUnlocked) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(containerAlpha),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Name + description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (nameRes != 0) stringResource(nameRes) else achievementId,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (descRes != 0) stringResource(descRes) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Token reward badge
            if (tokens > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUnlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                ) {
                    Text(
                        text = stringResource(R.string.achievement_tokens, tokens),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Token transaction row ──────────────────────────────────────────────────

@Composable
private fun TokenTransactionRow(tx: TokenTransaction) {
    val typeLabel = when {
        tx.type == "checkin" -> stringResource(R.string.token_type_checkin)
        tx.type == "achievement" -> stringResource(R.string.token_type_achievement)
        else -> stringResource(R.string.token_type_redeem)
    }
    val amountColor = if (tx.amount >= 0)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    val dateStr = remember(tx.timestamp) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(typeLabel, style = MaterialTheme.typography.bodyMedium)
            Text(dateStr, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (tx.amount >= 0) "+${tx.amount}" else "${tx.amount}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

// ── String resource helpers ────────────────────────────────────────────────

private fun achievementNameRes(id: String): Int = when (id) {
    "first_expense"    -> R.string.achievement_first_expense
    "set_budget"       -> R.string.achievement_set_budget
    "set_saving_goal"  -> R.string.achievement_set_saving_goal
    "first_loan"       -> R.string.achievement_first_loan
    "first_sync"       -> R.string.achievement_first_sync
    "first_ai_parse"   -> R.string.achievement_first_ai_parse
    "first_ai_analyze" -> R.string.achievement_first_ai_analyze
    "first_stock"      -> R.string.achievement_first_stock
    "first_income"     -> R.string.achievement_first_income
    "streak_7"         -> R.string.achievement_streak_7
    "streak_30"        -> R.string.achievement_streak_30
    "streak_90"        -> R.string.achievement_streak_90
    "streak_365"       -> R.string.achievement_streak_365
    "streak_730"       -> R.string.achievement_streak_730
    "budget_1"         -> R.string.achievement_budget_1
    "budget_3"         -> R.string.achievement_budget_3
    "budget_6"         -> R.string.achievement_budget_6
    "budget_9"         -> R.string.achievement_budget_9
    "budget_12"        -> R.string.achievement_budget_12
    "budget_18"        -> R.string.achievement_budget_18
    "budget_24"        -> R.string.achievement_budget_24
    else               -> 0
}

private fun achievementDescRes(id: String): Int = when (id) {
    "first_expense"    -> R.string.achievement_first_expense_desc
    "set_budget"       -> R.string.achievement_set_budget_desc
    "set_saving_goal"  -> R.string.achievement_set_saving_goal_desc
    "first_loan"       -> R.string.achievement_first_loan_desc
    "first_sync"       -> R.string.achievement_first_sync_desc
    "first_ai_parse"   -> R.string.achievement_first_ai_parse_desc
    "first_ai_analyze" -> R.string.achievement_first_ai_analyze_desc
    "first_stock"      -> R.string.achievement_first_stock_desc
    "first_income"     -> R.string.achievement_first_income_desc
    "streak_7"         -> R.string.achievement_streak_7_desc
    "streak_30"        -> R.string.achievement_streak_30_desc
    "streak_90"        -> R.string.achievement_streak_90_desc
    "streak_365"       -> R.string.achievement_streak_365_desc
    "streak_730"       -> R.string.achievement_streak_730_desc
    "budget_1"         -> R.string.achievement_budget_1_desc
    "budget_3"         -> R.string.achievement_budget_3_desc
    "budget_6"         -> R.string.achievement_budget_6_desc
    "budget_9"         -> R.string.achievement_budget_9_desc
    "budget_12"        -> R.string.achievement_budget_12_desc
    "budget_18"        -> R.string.achievement_budget_18_desc
    "budget_24"        -> R.string.achievement_budget_24_desc
    else               -> 0
}


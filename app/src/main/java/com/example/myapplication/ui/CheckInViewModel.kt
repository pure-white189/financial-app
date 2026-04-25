package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AchievementRepository
import com.example.myapplication.data.AchievementResult
import com.example.myapplication.data.AiExpenseParser
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.CheckInRepository
import com.example.myapplication.data.CheckInResult
import com.example.myapplication.data.CheckInStatusResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val aiExpenseParser = AiExpenseParser
    private val db = AppDatabase.getDatabase(application)
    private val checkInRepository = CheckInRepository(
        db.checkInDao(),
        db.tokenTransactionDao(),
        db.achievementDao(),
        aiExpenseParser
    )
    val achievementRepository = AchievementRepository(
        db.achievementDao(),
        checkInRepository
    )

    // ── Exposed state ──────────────────────────────────────────────────────

    private val _tokenBalance = MutableStateFlow(0)
    val tokenBalance: StateFlow<Int> = _tokenBalance.asStateFlow()

    val allTransactions = checkInRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements = achievementRepository.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCheckIns = checkInRepository.getAllCheckIns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _alreadyCheckedInToday = MutableStateFlow(false)
    val alreadyCheckedInToday: StateFlow<Boolean> = _alreadyCheckedInToday.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    // Backward-compatible alias for older UI code.
    val hasCheckedInToday: StateFlow<Boolean> = alreadyCheckedInToday

    // Result of latest check-in attempt (consumed by UI)
    private val _checkInResult = MutableStateFlow<CheckInResult?>(null)
    val checkInResult: StateFlow<CheckInResult?> = _checkInResult.asStateFlow()

    // Newly unlocked achievement IDs to show in UI (consumed by UI)
    private val _newAchievements = MutableStateFlow<List<String>>(emptyList())
    val newAchievements: StateFlow<List<String>> = _newAchievements.asStateFlow()

    private val _checkInMessage = MutableStateFlow<String?>(null)
    val checkInMessage: StateFlow<String?> = _checkInMessage.asStateFlow()

    fun clearCheckInMessage() {
        _checkInMessage.value = null
    }

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            loadCheckInStatus()
        }
        loadAchievements()
        checkBudgetAchievements()
    }

    // ── Actions ────────────────────────────────────────────────────────────

    fun refreshStreak() {
        viewModelScope.launch {
            _currentStreak.value = checkInRepository.getCurrentStreak()
            _alreadyCheckedInToday.value = checkInRepository.hasCheckedInToday()
        }
    }

    fun loadCheckInStatus() {
        viewModelScope.launch {
            val result = checkInRepository.aiExpenseParser.fetchCheckInStatus()
            when (result) {
                is CheckInStatusResult.Success -> {
                    _alreadyCheckedInToday.value = result.alreadyCheckedIn
                    _currentStreak.value = result.streak
                    _tokenBalance.value = result.balance
                }

                is CheckInStatusResult.NetworkError -> {
                    _checkInMessage.value = "network_error"
                }
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            loadCheckInStatus()
        }
    }

    fun refreshTokenBalance() {
        viewModelScope.launch {
            _tokenBalance.value = checkInRepository.getTokenBalance()
        }
    }

    fun checkIn() {
        viewModelScope.launch {
            val result = checkInRepository.checkIn()
            _checkInResult.value = result

            when (result) {
                is CheckInResult.Success -> {
                    _currentStreak.value = result.streak
                    _alreadyCheckedInToday.value = result.alreadyCheckedIn || result.baseTokens > 0 || result.bonusTokens > 0
                    if (result.alreadyCheckedIn) {
                        _checkInMessage.value = "already_checked_in"
                    } else {
                        _tokenBalance.value = result.newBalance
                        _alreadyCheckedInToday.value = true
                        _currentStreak.value = result.streak
                        _checkInMessage.value = if (result.bonusTokens > 0) {
                            "streak_bonus:${result.streak}:${result.baseTokens + result.bonusTokens}"
                        } else {
                            "success:${result.baseTokens}"
                        }
                    }

                    val streakAchievementId = when (result.streak) {
                        7 -> "streak_7"
                        30 -> "streak_30"
                        90 -> "streak_90"
                        365 -> "streak_365"
                        730 -> "streak_730"
                        else -> null
                    }

                    streakAchievementId?.let { id ->
                        val unlocked = achievementRepository.unlockBehaviorAchievement(id)
                        if (unlocked) {
                            _newAchievements.value = _newAchievements.value + id
                        }
                    }
                }

                is CheckInResult.NetworkError -> {
                    _checkInMessage.value = "network_error"
                }
            }
        }
    }


    // Backward-compatible entry point for existing UI call sites.
    fun performCheckIn() {
        checkIn()
    }
    fun consumeCheckInResult() {
        _checkInResult.value = null
    }

    fun consumeNewAchievements() {
        _newAchievements.value = emptyList()
    }

    /**
     * Unlock a behavior achievement from any screen.
     * Called from ExpenseViewModel, SyncViewModel, etc. after the relevant action.
     */
    fun unlockAchievement(achievementId: String) {
        viewModelScope.launch {
            val result = checkInRepository.unlockAchievement(achievementId)
            if (result is AchievementResult.Success && !result.alreadyUnlocked) {
                _newAchievements.value = _newAchievements.value + achievementId
                if (result.tokensEarned > 0) {
                    _tokenBalance.value = result.newBalance
                }
            }
        }
    }

    private fun loadAchievements() {
        // Achievement list is already driven by Flow/stateIn initialization.
    }

    private fun checkBudgetAchievements() {
        // Budget check with real monthly data is still handled by checkBudgetAchievementsOnStartup().
    }

    /**
     * Called on app startup from MainActivity.
     * Checks budget achievements using expense history.
     */
    fun checkBudgetAchievementsOnStartup(
        monthlyTotals: Map<String, Double>,
        monthlyBudget: Double
    ) {
        viewModelScope.launch {
            val newlyUnlocked = achievementRepository.checkBudgetAchievements(
                monthlyTotals,
                monthlyBudget,
                db.monthlyIncomeDao()
            )
            if (newlyUnlocked.isNotEmpty()) {
                _newAchievements.value = _newAchievements.value + newlyUnlocked
            }
        }
    }

    /**
     * Redeem tokens for AI features. Returns remaining balance or -1 if insufficient.
     */
    suspend fun redeemTokens(type: String): Int {
        return when (val result = checkInRepository.redeemTokensAndNotifyBackend(type, aiExpenseParser)) {
            is CheckInRepository.RedeemResult.Success -> {
                _tokenBalance.value = result.newBalance
                result.newBalance
            }

            CheckInRepository.RedeemResult.Failure -> -1
        }
    }

    suspend fun redeemTokensAndNotifyBackend(type: String): CheckInRepository.RedeemResult {
        val result = checkInRepository.redeemTokensAndNotifyBackend(type, aiExpenseParser)
        if (result is CheckInRepository.RedeemResult.Success) {
            _tokenBalance.value = result.newBalance
        }
        return result
    }
}


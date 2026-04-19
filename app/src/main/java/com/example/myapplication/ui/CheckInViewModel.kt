package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AchievementRepository
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.CheckInRepository
import com.example.myapplication.data.CheckInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val checkInRepository = CheckInRepository(
        db.checkInDao(),
        db.tokenTransactionDao()
    )
    val achievementRepository = AchievementRepository(
        db.achievementDao(),
        checkInRepository
    )

    // ── Exposed state ──────────────────────────────────────────────────────

    val tokenBalance: StateFlow<Int> = checkInRepository.getTokenBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTransactions = checkInRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements = achievementRepository.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCheckIns = checkInRepository.getAllCheckIns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _hasCheckedInToday = MutableStateFlow(false)
    val hasCheckedInToday: StateFlow<Boolean> = _hasCheckedInToday.asStateFlow()

    // Result of latest check-in attempt (consumed by UI)
    private val _checkInResult = MutableStateFlow<CheckInResult?>(null)
    val checkInResult: StateFlow<CheckInResult?> = _checkInResult.asStateFlow()

    // Newly unlocked achievement IDs to show in UI (consumed by UI)
    private val _newAchievements = MutableStateFlow<List<String>>(emptyList())
    val newAchievements: StateFlow<List<String>> = _newAchievements.asStateFlow()

    init {
        refreshStreak()
    }

    // ── Actions ────────────────────────────────────────────────────────────

    fun refreshStreak() {
        viewModelScope.launch {
            _currentStreak.value = checkInRepository.getCurrentStreak()
            _hasCheckedInToday.value = checkInRepository.hasCheckedInToday()
        }
    }

    fun performCheckIn() {
        viewModelScope.launch {
            val result = checkInRepository.checkIn()
            _checkInResult.value = result

            if (result is CheckInResult.Success) {
                _currentStreak.value = result.currentStreak
                _hasCheckedInToday.value = true

                // Unlock streak achievement if milestone reached
                result.streakAchievementId?.let { id ->
                    val unlocked = achievementRepository.unlockBehaviorAchievement(id)
                    if (unlocked) {
                        _newAchievements.value = _newAchievements.value + id
                    }
                }
            }
        }
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
            val unlocked = achievementRepository.unlockBehaviorAchievement(achievementId)
            if (unlocked) {
                _newAchievements.value = _newAchievements.value + achievementId
            }
        }
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
        val result = checkInRepository.redeemTokens(type)
        return when (result) {
            is CheckInRepository.RedeemResult.Success -> result.remaining
            is CheckInRepository.RedeemResult.InsufficientTokens -> -1
        }
    }
}


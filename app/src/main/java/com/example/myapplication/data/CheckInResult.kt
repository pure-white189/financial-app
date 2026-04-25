package com.example.myapplication.data

sealed class CheckInResult {
    data class Success(
        val alreadyCheckedIn: Boolean,
        val streak: Int,
        val baseTokens: Int,
        val bonusTokens: Int,
        val newBalance: Int
    ) : CheckInResult()

    data class NetworkError(val message: String) : CheckInResult()
}

sealed class AchievementResult {
    data class Success(
        val alreadyUnlocked: Boolean,
        val achievementId: String,
        val tokensEarned: Int,
        val newBalance: Int
    ) : AchievementResult()

    data class NetworkError(val message: String) : AchievementResult()
}

sealed class CheckInStatusResult {
    data class Success(
        val alreadyCheckedIn: Boolean,
        val streak: Int,
        val balance: Int
    ) : CheckInStatusResult()

    data class NetworkError(val message: String) : CheckInStatusResult()
}


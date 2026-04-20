package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE isDeleted = 0")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE achievementId = :id AND isDeleted = 0")
    suspend fun getAchievementById(id: String): Achievement?

    @Query("SELECT * FROM achievements WHERE unlockedAt > 0 AND isDeleted = 0")
    suspend fun getUnlockedAchievements(): List<Achievement>

    @Query("SELECT * FROM achievements WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<Achievement>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("DELETE FROM achievements")
    suspend fun clearAllAchievements()
}

package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllCheckIns(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE date = :date AND isDeleted = 0")
    suspend fun getCheckInByDate(date: String): CheckIn?

    @Query("SELECT COUNT(*) FROM check_ins WHERE isDeleted = 0")
    suspend fun getTotalCheckInCount(): Int

    @Query("SELECT * FROM check_ins WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<CheckIn>

    @Query("SELECT * FROM check_ins")
    suspend fun getAllCheckInsOnce(): List<CheckIn>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckIn)

    @Update
    suspend fun updateCheckIn(checkIn: CheckIn)

    @Delete
    suspend fun deleteCheckIn(checkIn: CheckIn)

    @Query("SELECT * FROM check_ins WHERE isDeleted = 0 ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentCheckIns(limit: Int): List<CheckIn>

    @Query("DELETE FROM check_ins")
    suspend fun clearAllCheckIns()
}

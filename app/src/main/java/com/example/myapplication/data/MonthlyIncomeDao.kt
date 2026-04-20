package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyIncomeDao {
    @Query("SELECT * FROM monthly_income WHERE isDeleted = 0 ORDER BY yearMonth DESC")
    fun getAllIncome(): Flow<List<MonthlyIncome>>

    @Query("SELECT * FROM monthly_income WHERE yearMonth = :yearMonth AND isDeleted = 0 LIMIT 1")
    suspend fun getIncomeForMonth(yearMonth: String): MonthlyIncome?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(income: MonthlyIncome)

    @Query("UPDATE monthly_income SET isDeleted = 1, updatedAt = :timestamp WHERE yearMonth = :yearMonth")
    suspend fun softDelete(yearMonth: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM monthly_income WHERE isDeleted = 0 ORDER BY yearMonth DESC")
    suspend fun getAllIncomeOnce(): List<MonthlyIncome>

    @Query("SELECT * FROM monthly_income WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<MonthlyIncome>

    @Query("DELETE FROM monthly_income")
    suspend fun clearAllMonthlyIncome()
}

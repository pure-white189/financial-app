package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiReportDao {
    @Query("SELECT * FROM ai_reports WHERE isDeleted = 0 ORDER BY yearMonth DESC")
    fun getAllReports(): Flow<List<AiReport>>

    @Query("SELECT * FROM ai_reports WHERE yearMonth = :yearMonth AND isDeleted = 0 LIMIT 1")
    suspend fun getReportForMonth(yearMonth: String): AiReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(report: AiReport)

    @Query("SELECT * FROM ai_reports WHERE isDeleted = 0")
    suspend fun getAllReportsOnce(): List<AiReport>

    @Query("SELECT * FROM ai_reports WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<AiReport>

    @Query("UPDATE ai_reports SET isDeleted = 1, updatedAt = :now WHERE yearMonth = :yearMonth")
    suspend fun softDelete(yearMonth: String, now: Long = System.currentTimeMillis())
}


package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingGoalDao {
    @Query("DELETE FROM saving_goals")
    suspend fun clearAllSavingGoals()

    @Query("SELECT * FROM saving_goals WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingGoal>>

    @Insert
    suspend fun insertGoal(goal: SavingGoal)

    @Update
    suspend fun updateGoal(goal: SavingGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingGoal)

    // --- 云同步新增 ---

    @Query("SELECT * FROM saving_goals WHERE isDeleted = 0")
    suspend fun getAllGoalsSnapshot(): List<SavingGoal>

    @Query("SELECT * FROM saving_goals WHERE isDeleted = 1")
    suspend fun getDeletedGoals(): List<SavingGoal>

    @Query("SELECT * FROM saving_goals WHERE firestoreId = :fid LIMIT 1")
    suspend fun getByFirestoreId(fid: String): SavingGoal?

    @Query("DELETE FROM saving_goals WHERE isDeleted = 1")
    suspend fun purgeDeleted()

    @Query("UPDATE saving_goals SET firestoreId = :fid WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Int, fid: String)
}
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingGoalDao {

	@Query("SELECT * FROM saving_goals ORDER BY createdAt DESC")
	fun getAllGoals(): Flow<List<SavingGoal>>

	@Insert
	suspend fun insertGoal(goal: SavingGoal)

	@Update
	suspend fun updateGoal(goal: SavingGoal)

	@Delete
	suspend fun deleteGoal(goal: SavingGoal)
}


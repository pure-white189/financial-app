package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

	@Query("SELECT * FROM loans ORDER BY date DESC")
	fun getAllLoans(): Flow<List<Loan>>

	@Query("SELECT * FROM loans WHERE isRepaid = 0 ORDER BY date DESC")
	fun getUnrepaidLoans(): Flow<List<Loan>>

	@Insert
	suspend fun insertLoan(loan: Loan)

	@Update
	suspend fun updateLoan(loan: Loan)

	@Delete
	suspend fun deleteLoan(loan: Loan)
}


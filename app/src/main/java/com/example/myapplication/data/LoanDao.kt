package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND isRepaid = 0 ORDER BY date DESC")
    fun getUnrepaidLoans(): Flow<List<Loan>>

    @Insert
    suspend fun insertLoan(loan: Loan)

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    // --- 云同步新增 ---

    @Query("SELECT * FROM loans WHERE isDeleted = 0")
    suspend fun getAllLoansSnapshot(): List<Loan>

    @Query("SELECT * FROM loans WHERE isDeleted = 1")
    suspend fun getDeletedLoans(): List<Loan>

    @Query("SELECT * FROM loans WHERE firestoreId = :fid LIMIT 1")
    suspend fun getByFirestoreId(fid: String): Loan?

    @Query("DELETE FROM loans WHERE isDeleted = 1")
    suspend fun purgeDeleted()

    @Query("UPDATE loans SET firestoreId = :fid WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Int, fid: String)
}
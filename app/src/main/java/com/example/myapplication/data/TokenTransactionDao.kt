package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenTransactionDao {
    @Query("SELECT * FROM token_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TokenTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM token_transactions WHERE isDeleted = 0")
    fun getTokenBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM token_transactions WHERE isDeleted = 0")
    suspend fun getTokenBalanceOnce(): Int

    @Query("SELECT * FROM token_transactions WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<TokenTransaction>

    @Query("SELECT * FROM token_transactions")
    suspend fun getAllTransactionsOnce(): List<TokenTransaction>

    @Query("SELECT * FROM token_transactions WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): TokenTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TokenTransaction)

    @Query("SELECT * FROM token_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int): List<TokenTransaction>

    @Query("DELETE FROM token_transactions")
    suspend fun clearAllTokenTransactions()
}

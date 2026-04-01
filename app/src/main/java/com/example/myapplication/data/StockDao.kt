package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query("SELECT * FROM stocks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllStocks(): Flow<List<Stock>>

    @Insert
    suspend fun insertStock(stock: Stock)

    @Update
    suspend fun updateStock(stock: Stock)

    @Delete
    suspend fun deleteStock(stock: Stock)

    // --- 云同步新增 ---

    @Query("SELECT * FROM stocks WHERE isDeleted = 0")
    suspend fun getAllStocksSnapshot(): List<Stock>

    @Query("SELECT * FROM stocks WHERE isDeleted = 1")
    suspend fun getDeletedStocks(): List<Stock>

    @Query("SELECT * FROM stocks WHERE firestoreId = :fid LIMIT 1")
    suspend fun getByFirestoreId(fid: String): Stock?

    @Query("DELETE FROM stocks WHERE isDeleted = 1")
    suspend fun purgeDeleted()

    @Query("UPDATE stocks SET firestoreId = :fid WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Int, fid: String)
}
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query("SELECT * FROM stocks ORDER BY createdAt DESC")
    fun getAllStocks(): Flow<List<Stock>>

    @Insert
    suspend fun insertStock(stock: Stock)

    @Update
    suspend fun updateStock(stock: Stock)

    @Delete
    suspend fun deleteStock(stock: Stock)
}


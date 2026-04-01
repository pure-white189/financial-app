package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE isDeleted = 0 AND date >= :startDate AND date <= :endDate")
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    // --- 云同步新增 ---

    // 查所有未被软删除的记录（全量上传用）
    @Query("SELECT * FROM expenses WHERE isDeleted = 0")
    suspend fun getAllExpensesSnapshot(): List<Expense>

    // 查所有需要同步删除到云端的记录
    @Query("SELECT * FROM expenses WHERE isDeleted = 1")
    suspend fun getDeletedExpenses(): List<Expense>

    // 按 firestoreId 查本地记录（下载合并用）
    @Query("SELECT * FROM expenses WHERE firestoreId = :fid LIMIT 1")
    suspend fun getByFirestoreId(fid: String): Expense?

    // 真正删除已完成软删除同步的记录
    @Query("DELETE FROM expenses WHERE isDeleted = 1")
    suspend fun purgeDeleted()

    // 回写 firestoreId（新记录首次上传后调用）
    @Query("UPDATE expenses SET firestoreId = :fid WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Int, fid: String)
}
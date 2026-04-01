package com.example.myapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SyncRepository(
    private val expenseDao: ExpenseDao,
    private val loanDao: LoanDao,
    private val savingGoalDao: SavingGoalDao,
    private val stockDao: StockDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("未登录，无法同步")

    private fun userCol(collection: String) =
        firestore.collection("users").document(uid).collection(collection)

    // ── 公开入口 ──────────────────────────────────
    suspend fun syncAll(): SyncResult {
        return try {
            val expenses  = syncExpenses()
            val loans     = syncLoans()
            val goals     = syncGoals()
            val stocks    = syncStocks()
            SyncResult.Success(
                uploaded = expenses.first + loans.first + goals.first + stocks.first,
                downloaded = expenses.second + loans.second + goals.second + stocks.second
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult.Error(e.message ?: "同步失败")

        }
    }

    // ── Expenses ──────────────────────────────────
    private suspend fun syncExpenses(): Pair<Int, Int> {
        val col = userCol("expenses")

        // 1. 上传本地所有记录（含软删除）
        val localAll = expenseDao.getAllExpensesSnapshot() + expenseDao.getDeletedExpenses()
        localAll.forEach { expense ->
            val fid = expense.firestoreId.ifEmpty { UUID.randomUUID().toString() }
            col.document(fid).set(expense.toMap(), SetOptions.merge()).await()
            if (expense.firestoreId.isEmpty()) {
                expenseDao.updateFirestoreId(expense.id, fid)
            }
        }

        // 2. 下载云端，LWW 合并
        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val cloudExpense = doc.toExpense()
            val local = expenseDao.getByFirestoreId(doc.id)
            when {
                local == null -> {
                    expenseDao.insertExpense(cloudExpense)
                    downloaded++
                }
                cloudExpense.updatedAt > local.updatedAt -> {
                    expenseDao.updateExpense(cloudExpense.copy(id = local.id))
                    downloaded++
                }
            }
        }

        // 3. 清理本地软删除记录（已上传云端）
        expenseDao.purgeDeleted()

        return Pair(localAll.size, downloaded)
    }

    // ── Loans ─────────────────────────────────────
    private suspend fun syncLoans(): Pair<Int, Int> {
        val col = userCol("loans")

        val localAll = loanDao.getAllLoansSnapshot() + loanDao.getDeletedLoans()
        localAll.forEach { loan ->
            val fid = loan.firestoreId.ifEmpty { UUID.randomUUID().toString() }
            col.document(fid).set(loan.toMap(), SetOptions.merge()).await()
            if (loan.firestoreId.isEmpty()) {
                loanDao.updateFirestoreId(loan.id, fid)
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val cloudLoan = doc.toLoan()
            val local = loanDao.getByFirestoreId(doc.id)
            when {
                local == null -> { loanDao.insertLoan(cloudLoan); downloaded++ }
                cloudLoan.updatedAt > local.updatedAt -> {
                    loanDao.updateLoan(cloudLoan.copy(id = local.id)); downloaded++
                }
            }
        }

        loanDao.purgeDeleted()
        return Pair(localAll.size, downloaded)
    }

    // ── SavingGoals ───────────────────────────────
    private suspend fun syncGoals(): Pair<Int, Int> {
        val col = userCol("saving_goals")

        val localAll = savingGoalDao.getAllGoalsSnapshot() + savingGoalDao.getDeletedGoals()
        localAll.forEach { goal ->
            val fid = goal.firestoreId.ifEmpty { UUID.randomUUID().toString() }
            col.document(fid).set(goal.toMap(), SetOptions.merge()).await()
            if (goal.firestoreId.isEmpty()) {
                savingGoalDao.updateFirestoreId(goal.id, fid)
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val cloudGoal = doc.toSavingGoal()
            val local = savingGoalDao.getByFirestoreId(doc.id)
            when {
                local == null -> { savingGoalDao.insertGoal(cloudGoal); downloaded++ }
                cloudGoal.updatedAt > local.updatedAt -> {
                    savingGoalDao.updateGoal(cloudGoal.copy(id = local.id)); downloaded++
                }
            }
        }

        savingGoalDao.purgeDeleted()
        return Pair(localAll.size, downloaded)
    }

    // ── Stocks ────────────────────────────────────
    private suspend fun syncStocks(): Pair<Int, Int> {
        val col = userCol("stocks")

        val localAll = stockDao.getAllStocksSnapshot() + stockDao.getDeletedStocks()
        localAll.forEach { stock ->
            val fid = stock.firestoreId.ifEmpty { UUID.randomUUID().toString() }
            col.document(fid).set(stock.toMap(), SetOptions.merge()).await()
            if (stock.firestoreId.isEmpty()) {
                stockDao.updateFirestoreId(stock.id, fid)
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val cloudStock = doc.toStock()
            val local = stockDao.getByFirestoreId(doc.id)
            when {
                local == null -> { stockDao.insertStock(cloudStock); downloaded++ }
                cloudStock.updatedAt > local.updatedAt -> {
                    stockDao.updateStock(cloudStock.copy(id = local.id)); downloaded++
                }
            }
        }

        stockDao.purgeDeleted()
        return Pair(localAll.size, downloaded)
    }
}

// ── 结果类 ────────────────────────────────────────
sealed class SyncResult {
    data class Success(val uploaded: Int, val downloaded: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
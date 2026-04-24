package com.example.myapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SyncRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val templateDao: ExpenseTemplateDao,
    private val loanDao: LoanDao,
    private val savingGoalDao: SavingGoalDao,
    private val stockDao: StockDao,
    private val monthlyIncomeDao: MonthlyIncomeDao,
    private val checkInDao: CheckInDao,
    private val achievementDao: AchievementDao,
    private val tokenTransactionDao: TokenTransactionDao,
    private val aiReportDao: AiReportDao
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
            val expenses     = syncExpenses()
            val loans        = syncLoans()
            val goals        = syncGoals()
            val stocks       = syncStocks()
            val income       = syncMonthlyIncome()
            val checkIns     = syncCheckIns()
            val achievements = syncAchievements()
            val tokens       = syncTokenTransactions()
            val reports      = syncAiReports()
            val categories   = syncCustomCategories()
            val templates    = syncExpenseTemplates()
            SyncResult.Success(
                uploaded = expenses.first + loans.first + goals.first +
                        stocks.first + income.first + checkIns.first +
                        achievements.first + tokens.first + reports.first +
                        categories.first + templates.first,
                downloaded = expenses.second + loans.second + goals.second +
                        stocks.second + income.second + checkIns.second +
                        achievements.second + tokens.second + reports.second +
                        categories.second + templates.second
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

    // ── Monthly Income ───────────────────────────
    private suspend fun syncMonthlyIncome(): Pair<Int, Int> {
        val col = userCol("monthly_income")

        val localAll = monthlyIncomeDao.getAllIncomeOnce()
        localAll.forEach { income ->
            val fid = income.firestoreId.ifEmpty { UUID.randomUUID().toString() }
            col.document(fid).set(income.toFirestoreMap(), SetOptions.merge()).await()
            if (income.firestoreId.isEmpty()) {
                monthlyIncomeDao.insertOrUpdate(income.copy(firestoreId = fid))
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val data = doc.data
            val cloudIncome = data.toMonthlyIncome(doc.id)
            val local = monthlyIncomeDao.getIncomeForMonth(cloudIncome.yearMonth)
            when {
                local == null -> {
                    monthlyIncomeDao.insertOrUpdate(cloudIncome)
                    downloaded++
                }
                cloudIncome.updatedAt > local.updatedAt -> {
                    monthlyIncomeDao.insertOrUpdate(cloudIncome)
                    downloaded++
                }
            }
        }

        return Pair(localAll.size, downloaded)
    }

    // ── CheckIns ──────────────────────────────────────
    private suspend fun syncCheckIns(): Pair<Int, Int> {
        val col = userCol("check_ins")

        val localAll = checkInDao.getAllCheckInsOnce()
        localAll.forEach { checkIn ->
            val fid = checkIn.firestoreId.ifEmpty { checkIn.date }
            col.document(fid).set(checkIn.toFirestoreMap(), SetOptions.merge()).await()
            if (checkIn.firestoreId.isEmpty()) {
                checkInDao.insertCheckIn(checkIn.copy(firestoreId = fid))
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val data = doc.data ?: return@forEach
            val cloudCheckIn = data.toCheckIn(doc.id)
            val local = checkInDao.getCheckInByDate(cloudCheckIn.date)
            when {
                local == null -> {
                    checkInDao.insertCheckIn(cloudCheckIn)
                    downloaded++
                }
                cloudCheckIn.updatedAt > local.updatedAt -> {
                    checkInDao.insertCheckIn(cloudCheckIn)
                    downloaded++
                }
            }
        }

        return Pair(localAll.size, downloaded)
    }

    // ── Achievements ──────────────────────────────────
    private suspend fun syncAchievements(): Pair<Int, Int> {
        val col = userCol("achievements")

        val localAll = achievementDao.getAllAchievementsOnce()
        localAll.forEach { achievement ->
            val fid = achievement.firestoreId.ifEmpty { achievement.achievementId }
            col.document(fid).set(achievement.toFirestoreMap(), SetOptions.merge()).await()
            if (achievement.firestoreId.isEmpty()) {
                achievementDao.insertAchievement(achievement.copy(firestoreId = fid))
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val data = doc.data ?: return@forEach
            val cloudAchievement = data.toAchievement(doc.id)
            val local = achievementDao.getAchievementById(cloudAchievement.achievementId)
            when {
                local == null -> {
                    achievementDao.insertAchievement(cloudAchievement)
                    downloaded++
                }
                cloudAchievement.updatedAt > local.updatedAt -> {
                    achievementDao.insertAchievement(cloudAchievement)
                    downloaded++
                }
            }
        }

        return Pair(localAll.size, downloaded)
    }

    // ── TokenTransactions ─────────────────────────────
    private suspend fun syncTokenTransactions(): Pair<Int, Int> {
        val col = userCol("token_transactions")

        val localAll = tokenTransactionDao.getAllTransactionsOnce()
        localAll.forEach { tx ->
            val fid = tx.firestoreId.ifEmpty { UUID.randomUUID().toString() }
            col.document(fid).set(tx.toFirestoreMap(), SetOptions.merge()).await()
            if (tx.firestoreId.isEmpty()) {
                tokenTransactionDao.insertTransaction(tx.copy(firestoreId = fid))
            }
        }

        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val data = doc.data ?: return@forEach
            val cloudTx = data.toTokenTransaction(doc.id)
            val local = tokenTransactionDao.getByFirestoreId(doc.id)
            when {
                local == null -> {
                    tokenTransactionDao.insertTransaction(cloudTx)
                    downloaded++
                }
                cloudTx.updatedAt > local.updatedAt -> {
                    tokenTransactionDao.insertTransaction(cloudTx)
                    downloaded++
                }
            }
        }

        return Pair(localAll.size, downloaded)
    }

    private suspend fun syncAiReports(): Pair<Int, Int> {
        val uid = auth.currentUser?.uid ?: return Pair(0, 0)
        val col = firestore.collection("users").document(uid).collection("ai_reports")
        var uploaded = 0
        var downloaded = 0

        val localReports = aiReportDao.getAllReportsOnce()
        val knownFirestoreIds = localReports.mapNotNull { it.firestoreId.takeIf { fid -> fid.isNotBlank() } }.toMutableSet()
        for (report in localReports) {
            val id = report.firestoreId.ifBlank { UUID.randomUUID().toString() }
            val localReport = if (report.firestoreId.isBlank()) {
                report.copy(firestoreId = id, updatedAt = System.currentTimeMillis())
            } else {
                report
            }
            knownFirestoreIds.add(id)
            val ref = col.document(id)
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(localReport.toFirestoreMap()).await()
                aiReportDao.insertOrUpdate(localReport)
                uploaded++
            } else {
                val remoteUpdatedAt = (snap.data?.get("updatedAt") as? Number)?.toLong() ?: 0L
                if (localReport.updatedAt >= remoteUpdatedAt) {
                    ref.set(localReport.toFirestoreMap()).await()
                    uploaded++
                } else {
                    val remote = snap.data!!.toAiReport(id)
                    aiReportDao.insertOrUpdate(remote)
                    downloaded++
                }
            }
        }

        val remoteSnaps = col.get().await()
        for (snap in remoteSnaps.documents) {
            val id = snap.id
            if (!knownFirestoreIds.contains(id)) {
                val remote = snap.data!!.toAiReport(id)
                if (remote.isDeleted == 0) {
                    aiReportDao.insertOrUpdate(remote)
                    downloaded++
                }
            }
        }
        return Pair(uploaded, downloaded)
    }

    private suspend fun syncCustomCategories(): Pair<Int, Int> {
        val col = userCol("categories")
        val localCustom = categoryDao.getAllCustomCategoriesOnce()

        // Upload local custom categories to Firestore
        localCustom.forEach { category ->
            val fid = category.firestoreId.ifEmpty { "cat_${category.id}" }
            col.document(fid).set(category.toFirestoreMap(), SetOptions.merge()).await()
            if (category.firestoreId.isEmpty()) {
                categoryDao.upsert(category.copy(firestoreId = fid))
            }
        }

        // Download and merge from Firestore
        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val data = doc.data ?: return@forEach
            val cloudCategory = data.toCategory(doc.id)
            if (cloudCategory.isDefault) return@forEach  // skip default categories

            // Find local match by firestoreId
            val local = localCustom.find { it.firestoreId == doc.id }
            when {
                local == null -> {
                    if (!cloudCategory.isDeleted) {
                        categoryDao.upsert(cloudCategory)
                        downloaded++
                    }
                }
                cloudCategory.updatedAt > local.updatedAt -> {
                    categoryDao.upsert(cloudCategory.copy(id = local.id))
                    downloaded++
                }
            }
        }
        return Pair(localCustom.size, downloaded)
    }

    private suspend fun syncExpenseTemplates(): Pair<Int, Int> {
        val col = userCol("expense_templates")
        val localAll = templateDao.getAllTemplatesIncludingDeleted()

        // Upload local templates — backfill categoryKey from categoryDao if empty
        localAll.forEach { template ->
            val fid = template.firestoreId.ifEmpty { "tpl_${template.id}" }
            val withKey = if (template.categoryKey.isEmpty()) {
                val cat = categoryDao.getCategoryById(template.categoryId)
                template.copy(
                    firestoreId = fid,
                    categoryKey = cat?.categoryKey ?: cat?.name ?: ""
                )
            } else {
                template.copy(firestoreId = fid)
            }
            col.document(fid).set(withKey.toFirestoreMap(), SetOptions.merge()).await()
            if (template.firestoreId.isEmpty()) {
                templateDao.upsert(withKey)
            }
        }

        // Download and merge from Firestore
        val cloudDocs = col.get().await()
        var downloaded = 0
        cloudDocs.forEach { doc ->
            val data = doc.data ?: return@forEach
            val cloudKey = data["categoryKey"] as? String ?: ""

            // Resolve categoryId from categoryKey
            val resolvedCategoryId = if (cloudKey.isNotEmpty()) {
                categoryDao.getDefaultCategoryByKey(cloudKey)?.id
                    ?: categoryDao.getCustomCategoryByName(cloudKey)?.id
                    ?: 0
            } else 0

            val cloudTemplate = data.toExpenseTemplate(doc.id, resolvedCategoryId)
            val local = localAll.find { it.firestoreId == doc.id }
            when {
                local == null -> {
                    if (!cloudTemplate.isDeleted) {
                        templateDao.upsert(cloudTemplate)
                        downloaded++
                    }
                }
                cloudTemplate.updatedAt > local.updatedAt -> {
                    templateDao.upsert(cloudTemplate.copy(id = local.id))
                    downloaded++
                }
            }
        }
        return Pair(localAll.size, downloaded)
    }
}

// ── 结果类 ────────────────────────────────────────
sealed class SyncResult {
    data class Success(val uploaded: Int, val downloaded: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
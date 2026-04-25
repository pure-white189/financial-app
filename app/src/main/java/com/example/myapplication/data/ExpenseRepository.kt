package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val expenseTemplateDao: ExpenseTemplateDao,
    private val loanDao: LoanDao,
    private val savingGoalDao: SavingGoalDao,
    private val stockDao: StockDao,
    private val monthlyIncomeDao: MonthlyIncomeDao,
    private val checkInDao: CheckInDao,
    private val achievementDao: AchievementDao,
    private val tokenTransactionDao: TokenTransactionDao,
    private val aiReportDao: AiReportDao
) {
    // 类别相关
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteCategory(category: Category) =
        categoryDao.softDelete(category.id, System.currentTimeMillis())

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)
    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.updateExpense(
        expense.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
    )
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalExpenseByDateRange(startDate, endDate)

    fun getAllTemplates(): Flow<List<ExpenseTemplate>> = expenseTemplateDao.getAllTemplates()
    suspend fun insertTemplate(template: ExpenseTemplate) = expenseTemplateDao.insertTemplate(template)
    suspend fun deleteTemplate(template: ExpenseTemplate) =
        expenseTemplateDao.softDelete(template.id, System.currentTimeMillis())
    suspend fun updateTemplate(template: ExpenseTemplate) = expenseTemplateDao.updateTemplate(template)

    fun getAllLoans(): Flow<List<Loan>> = loanDao.getAllLoans()
    fun getUnrepaidLoans(): Flow<List<Loan>> = loanDao.getUnrepaidLoans()
    suspend fun insertLoan(loan: Loan) = loanDao.insertLoan(loan)
    suspend fun updateLoan(loan: Loan) = loanDao.updateLoan(loan)
    suspend fun deleteLoan(loan: Loan) = loanDao.updateLoan(
        loan.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
    )

    fun getAllGoals(): Flow<List<SavingGoal>> = savingGoalDao.getAllGoals()
    suspend fun insertGoal(goal: SavingGoal) = savingGoalDao.insertGoal(goal)
    suspend fun updateGoal(goal: SavingGoal) = savingGoalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: SavingGoal) = savingGoalDao.updateGoal(
        goal.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
    )

    fun getAllStocks(): Flow<List<Stock>> = stockDao.getAllStocks()
    suspend fun insertStock(stock: Stock) = stockDao.insertStock(stock)
    suspend fun updateStock(stock: Stock) = stockDao.updateStock(stock)
    suspend fun deleteStock(stock: Stock) = stockDao.updateStock(
        stock.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
    )

    // 清除所有消费记录
    suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
    }

    // 清除所有模板
    suspend fun clearAllTemplates() {
        expenseTemplateDao.clearAllTemplates()
    }

    // 清除所有自定义类别（不删除默认类别）
    suspend fun clearCustomCategories() {
        categoryDao.clearCustomCategories()
    }

    suspend fun clearAllLoans() = loanDao.clearAllLoans()
    suspend fun clearAllSavingGoals() = savingGoalDao.clearAllSavingGoals()
    suspend fun clearAllStocks() = stockDao.clearAllStocks()
    suspend fun clearAllMonthlyIncome() = monthlyIncomeDao.clearAllMonthlyIncome()
    suspend fun clearAllCheckIns() = checkInDao.clearAllCheckIns()
    suspend fun clearAllAchievements() = achievementDao.clearAllAchievements()
    suspend fun clearAllTokenTransactions() = tokenTransactionDao.clearAllTokenTransactions()
    suspend fun clearAllAiReports() = aiReportDao.clearAllAiReports()
}
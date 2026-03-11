package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val expenseTemplateDao: ExpenseTemplateDao
) {
    // 类别相关
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)
    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalExpenseByDateRange(startDate, endDate)

    fun getAllTemplates(): Flow<List<ExpenseTemplate>> = expenseTemplateDao.getAllTemplates()
    suspend fun insertTemplate(template: ExpenseTemplate) = expenseTemplateDao.insertTemplate(template)
    suspend fun deleteTemplate(template: ExpenseTemplate) = expenseTemplateDao.deleteTemplate(template)
    suspend fun updateTemplate(template: ExpenseTemplate) = expenseTemplateDao.updateTemplate(template)

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
}
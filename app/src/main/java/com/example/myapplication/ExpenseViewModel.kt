package com.example.myapplication

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.ExpenseTemplate
import com.example.myapplication.data.AiReport
import com.example.myapplication.data.AiExpenseParser
import com.example.myapplication.data.AiReportDao
import com.example.myapplication.data.Loan
import com.example.myapplication.data.MonthlyIncome
import com.example.myapplication.data.MonthlyIncomeDao
import com.example.myapplication.data.SavingGoal
import com.example.myapplication.data.Stock
import com.example.myapplication.data.ThemePreferences
import com.example.myapplication.data.dataStore
import com.example.myapplication.data.getCurrencySymbol
import com.example.myapplication.utils.RecommendationEngine
import com.example.myapplication.utils.RecommendationTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val themePreferences: ThemePreferences,
    private val monthlyIncomeDao: MonthlyIncomeDao,
    private val aiReportDao: AiReportDao,
    private val appContext: Context
) : ViewModel() {

    private val aiExpenseParser = AiExpenseParser

    // 所有类别
    val categories = repository.getAllCategories()

    // 所有消费记录
    val expenses = repository.getAllExpenses()
    val loans = repository.getAllLoans()
    val savingGoals = repository.getAllGoals()
    val stocks = repository.getAllStocks()

    val templates = repository.getAllTemplates()

    // --- Income ---
    val allIncome: StateFlow<List<MonthlyIncome>> = monthlyIncomeDao.getAllIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<AiReport>> = aiReportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 本月总支出
    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal: StateFlow<Double> = _monthlyTotal.asStateFlow()

    private val _totalStockValue = MutableStateFlow(0.0)
    val totalStockValue: StateFlow<Double> = _totalStockValue.asStateFlow()

    val currencySymbol: StateFlow<String> = themePreferences.selectedCurrency
        .map { currency -> getCurrencySymbol(currency) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "HK$")

    val parseUsedToday: StateFlow<Int> = AiExpenseParser.parseUsedToday
    val parseLimit: StateFlow<Int?> = AiExpenseParser.parseLimitFlow
    val aiPlan: StateFlow<String> = AiExpenseParser.planFlow
    val lastAnalysisRecommendationType: MutableStateFlow<String?> = MutableStateFlow(null)
    val lastAnalysisRecommendationStat: MutableStateFlow<String?> = MutableStateFlow(null)

    val todayRecommendation: MutableStateFlow<RecommendationTrigger?> = MutableStateFlow(null)
    val recommendationsJson: MutableStateFlow<String?> = MutableStateFlow(null)
    val insightDismissedDate: MutableStateFlow<String> = MutableStateFlow("")

    init {
        loadMonthlyTotal()
        loadTotalStockValue()
        computeTodayRecommendation()
        viewModelScope.launch {
            loadRecommendationsJson(appContext)
        }
        viewModelScope.launch {
            appContext.dataStore.data.collect { prefs ->
                insightDismissedDate.value = prefs[ThemePreferences.INSIGHT_DISMISSED_DATE_KEY] ?: ""
            }
        }
    }

    fun dismissTodayInsight() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        insightDismissedDate.value = today
        viewModelScope.launch {
            appContext.dataStore.edit { it[ThemePreferences.INSIGHT_DISMISSED_DATE_KEY] = today }
        }
    }

    fun computeTodayRecommendation() {
        viewModelScope.launch {
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentYearMonth = monthFormat.format(Date())

            val allExpenses = expenses.first()
            val currentMonthExpenses = allExpenses.filter { expense ->
                monthFormat.format(Date(expense.date)) == currentYearMonth
            }

            val monthlyBudget = (themePreferences.monthlyBudget.first() ?: 0.0).toFloat()
            val currentMonthSpend = currentMonthExpenses.sumOf { it.amount }.toFloat()
            val categoryKeyMap = categories.first().associate { it.id.toLong() to it.categoryKey }
            val stockCount = stocks.first().size
            val streakDays = 0
            val totalExpenseCount = allExpenses.size
            val symbol = currencySymbol.value

            todayRecommendation.value = RecommendationEngine.evaluate(
                expenses = currentMonthExpenses,
                monthlyBudget = monthlyBudget,
                currentMonthSpend = currentMonthSpend,
                stockCount = stockCount,
                streakDays = streakDays,
                totalExpenseCount = totalExpenseCount,
                currencySymbol = symbol,
                categoryKeyMap = categoryKeyMap
            )
        }
    }

    suspend fun loadRecommendationsJson(context: Context) {
        recommendationsJson.value = aiExpenseParser.loadRecommendations(context)
    }

    suspend fun analyzeExpensesWithRecommendation(
        expenses: List<AiExpenseParser.ExpenseSummary>,
        month: String,
        lang: String
    ): Result<String> {
        val result = aiExpenseParser.analyzeExpensesDetailed(expenses = expenses, month = month, lang = lang)
        return result.fold(
            onSuccess = { detailed ->
                lastAnalysisRecommendationType.value = detailed.recommendationType
                lastAnalysisRecommendationStat.value = detailed.recommendationStat
                Result.success(detailed.analysis)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private fun loadTotalStockValue() {
        viewModelScope.launch {
            repository.getAllStocks().collect { stockList ->
                _totalStockValue.value = stockList.sumOf { it.currentPrice * it.shares }
            }
        }
    }

    // 加载本月总支出
    private fun loadMonthlyTotal() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis

            repository.getTotalExpenseByDateRange(startOfMonth, endOfMonth).collect { total ->
                _monthlyTotal.value = total ?: 0.0
            }
        }
    }

    // 添加消费记录
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    // 删除消费记录
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(
                expense.copy(
                    isDeleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    // 添加类别
    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }

    // 删除类别
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // 恢复默认类别（key 化版本）
    fun restoreDefaultCategories() {
        viewModelScope.launch {
            val defaultCategories = listOf(
                Category(name = "food",          categoryKey = "food",          iconName = "restaurant",     color = "#FF5722", isDefault = true),
                Category(name = "transport",     categoryKey = "transport",     iconName = "directions_car", color = "#2196F3", isDefault = true),
                Category(name = "shopping",      categoryKey = "shopping",      iconName = "shopping_cart",  color = "#E91E63", isDefault = true),
                Category(name = "entertainment", categoryKey = "entertainment", iconName = "movie",           color = "#9C27B0", isDefault = true),
                Category(name = "health",        categoryKey = "health",        iconName = "local_hospital",  color = "#F44336", isDefault = true),
                Category(name = "education",     categoryKey = "education",     iconName = "school",          color = "#00BCD4", isDefault = true),
                Category(name = "housing",       categoryKey = "housing",       iconName = "home",            color = "#FF9800", isDefault = true),
                Category(name = "other",         categoryKey = "other",         iconName = "more_horiz",      color = "#607D8B", isDefault = true),
            )

            // 用 categoryKey 查重，避免重复插入（原来用 name 查重，key 化后 name 是英文 key 字符串）
            val existingKeys = repository.getAllCategories().first()
                .map { it.categoryKey }
                .toSet()

            defaultCategories.forEach { category ->
                if (category.categoryKey !in existingKeys) {
                    repository.insertCategory(category)
                }
            }
        }
    }

    // 根据 ID 获取类别
    suspend fun getCategoryById(id: Int): Category? {
        return repository.getCategoryById(id)
    }

    fun addTemplate(template: ExpenseTemplate) {
        viewModelScope.launch {
            repository.insertTemplate(template)
        }
    }

    fun deleteTemplate(template: ExpenseTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun updateTemplate(template: ExpenseTemplate) {
        viewModelScope.launch {
            repository.updateTemplate(template)
        }
    }

    fun addLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(
                loan.copy(
                    isDeleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun markAsRepaid(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(loan.copy(isRepaid = true))
        }
    }

    fun addGoal(goal: SavingGoal) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingGoal) {
        viewModelScope.launch {
            repository.updateGoal(
                goal.copy(
                    isDeleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateGoal(goal: SavingGoal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun addDeposit(goal: SavingGoal, amount: Double) {
        viewModelScope.launch {
            val nextAmount = goal.currentAmount + amount
            repository.updateGoal(
                goal.copy(
                    currentAmount = nextAmount,
                    isCompleted = nextAmount >= goal.targetAmount
                )
            )
        }
    }

    fun depositToGoal(goalId: Int, amount: Double) {
        viewModelScope.launch {
            val targetGoal = repository.getAllGoals().first().firstOrNull { it.id == goalId } ?: return@launch
            val nextAmount = targetGoal.currentAmount + amount
            repository.updateGoal(
                targetGoal.copy(
                    currentAmount = nextAmount,
                    isCompleted = nextAmount >= targetGoal.targetAmount
                )
            )
        }
    }

    fun addStock(stock: Stock) {
        viewModelScope.launch {
            repository.insertStock(stock)
        }
    }

    fun deleteStock(stock: Stock) {
        viewModelScope.launch {
            repository.updateStock(
                stock.copy(
                    isDeleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateStockPrice(stock: Stock, newPrice: Double) {
        viewModelScope.launch {
            repository.updateStock(
                stock.copy(
                    currentPrice = newPrice,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateStock(stock: Stock) {
        viewModelScope.launch {
            repository.updateStock(stock)
        }
    }

    fun getCurrentYearMonth(): String {
        val now = java.util.Calendar.getInstance()
        return "%d-%02d".format(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
    }

    fun getIncomeForCurrentMonth(): StateFlow<MonthlyIncome?> {
        val yearMonth = getCurrentYearMonth()
        return monthlyIncomeDao.getAllIncome()
            .map { list -> list.firstOrNull { it.yearMonth == yearMonth } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun upsertIncome(yearMonth: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            val existing = monthlyIncomeDao.getIncomeForMonth(yearMonth)
            val income = MonthlyIncome(
                yearMonth = yearMonth,
                amount = amount,
                note = note,
                firestoreId = existing?.firestoreId ?: "",
                updatedAt = System.currentTimeMillis(),
                isDeleted = false
            )
            monthlyIncomeDao.insertOrUpdate(income)
        }
    }

    suspend fun saveReport(yearMonth: String, content: String) {
        val id = java.util.UUID.randomUUID().toString()
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        aiReportDao.insertOrUpdate(
            AiReport(
                id = 0,
                yearMonth = yearMonth,
                content = content,
                generatedAt = generatedAt,
                firestoreId = id,
                updatedAt = System.currentTimeMillis(),
                isDeleted = 0
            )
        )
    }

    // 切换模板置顶状态
    fun toggleTemplatePinned(template: ExpenseTemplate) {
        viewModelScope.launch {
            // 如果要置顶，先检查已置顶的数量
            if (!template.isPinned) {
                val pinnedCount = repository.getAllTemplates().first()
                    .count { it.isPinned }

                if (pinnedCount >= 3) {
                    // 已经有 3 个置顶了，不能再置顶
                    return@launch
                }
            }

            repository.updateTemplate(
                template.copy(isPinned = !template.isPinned)
            )
        }
    }

    // 清除所有消费记录
    fun clearAllExpenses() {
        viewModelScope.launch {
            repository.clearAllExpenses()
        }
    }

    // 清除所有模板
    fun clearAllTemplates() {
        viewModelScope.launch {
            repository.clearAllTemplates()
        }
    }

    // 清除所有自定义类别
    fun clearCustomCategories() {
        viewModelScope.launch {
            repository.clearCustomCategories()
        }
    }

    // 清除所有数据（保留默认类别）
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllExpenses()
            repository.clearAllTemplates()
            repository.clearCustomCategories()
        }
    }

    // 从模板创建消费记录
    fun createExpenseFromTemplate(template: ExpenseTemplate) {
        viewModelScope.launch {
            val expense = Expense(
                amount = template.amount,
                categoryId = template.categoryId,
                date = System.currentTimeMillis(),
                note = template.note
            )
            repository.insertExpense(expense)
        }
    }

    // ===== 数据导出相关方法 =====

    /**
     * 获取指定时间范围的消费记录（同步方法，用于导出页面）
     */
    suspend fun getExpensesByDateRange(startDate: Long, endDate: Long): List<Expense> {
        return repository.getExpensesByDateRange(startDate, endDate).first()
    }

    /**
     * 获取所有类别的 Map（用于导出时查找类别名称）
     */
    suspend fun getAllCategories(): List<Category> {
        return repository.getAllCategories().first()
    }

}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val themePreferences: ThemePreferences,
    private val monthlyIncomeDao: MonthlyIncomeDao,
    private val aiReportDao: AiReportDao,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, themePreferences, monthlyIncomeDao, aiReportDao, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
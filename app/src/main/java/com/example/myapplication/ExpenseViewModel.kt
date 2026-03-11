package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.ExpenseTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // 所有类别
    val categories = repository.getAllCategories()

    // 所有消费记录
    val expenses = repository.getAllExpenses()

    val templates = repository.getAllTemplates()
    // 本月总支出
    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal: StateFlow<Double> = _monthlyTotal.asStateFlow()

    init {
        loadMonthlyTotal()
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
            repository.deleteExpense(expense)
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

    // 恢复默认类别
    fun restoreDefaultCategories() {
        viewModelScope.launch {
            val defaultCategories = listOf(
                Category(name = "餐饮", iconName = "restaurant", color = "#FF5722", isDefault = true),
                Category(name = "交通", iconName = "directions_car", color = "#2196F3", isDefault = true),
                Category(name = "购物", iconName = "shopping_cart", color = "#E91E63", isDefault = true),
                Category(name = "娱乐", iconName = "movie", color = "#9C27B0", isDefault = true),
                Category(name = "医疗", iconName = "local_hospital", color = "#F44336", isDefault = true),
                Category(name = "教育", iconName = "school", color = "#00BCD4", isDefault = true),
                Category(name = "居住", iconName = "home", color = "#FF9800", isDefault = true),
                Category(name = "其他", iconName = "more_horiz", color = "#607D8B", isDefault = true)
            )

            // 获取现有类别名称
            val existingCategories = repository.getAllCategories().first()
            val existingNames = existingCategories.map { it.name }.toSet()

            // 只添加不存在的默认类别
            defaultCategories.forEach { category ->
                if (category.name !in existingNames) {
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

}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
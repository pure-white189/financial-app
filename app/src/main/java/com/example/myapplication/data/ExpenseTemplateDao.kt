package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTemplateDao {
    @Query("SELECT * FROM expense_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<ExpenseTemplate>>

    @Insert
    suspend fun insertTemplate(template: ExpenseTemplate)

    @Delete
    suspend fun deleteTemplate(template: ExpenseTemplate)

    @Query("DELETE FROM expense_templates")
    suspend fun clearAllTemplates()

    @Update
    suspend fun updateTemplate(template: ExpenseTemplate)
}
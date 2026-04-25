package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTemplateDao {
    @Query("SELECT * FROM expense_templates WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<ExpenseTemplate>>

    @Insert
    suspend fun insertTemplate(template: ExpenseTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: ExpenseTemplate)

    @Query("SELECT * FROM expense_templates WHERE isDeleted = 0")
    suspend fun getAllTemplatesOnce(): List<ExpenseTemplate>

    @Query("SELECT * FROM expense_templates")
    suspend fun getAllTemplatesIncludingDeleted(): List<ExpenseTemplate>

    @Query("UPDATE expense_templates SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM expense_templates WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<ExpenseTemplate>

    @Delete
    suspend fun deleteTemplate(template: ExpenseTemplate)

    suspend fun delete(template: ExpenseTemplate) = deleteTemplate(template)

    @Query("DELETE FROM expense_templates")
    suspend fun clearAllTemplates()

    @Update
    suspend fun updateTemplate(template: ExpenseTemplate)
}
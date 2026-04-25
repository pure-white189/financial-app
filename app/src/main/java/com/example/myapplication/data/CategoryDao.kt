package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE isDefault = 0 AND isDeleted = 0")
    suspend fun getCustomCategoriesOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE isDefault = 0")
    suspend fun getAllCustomCategoriesOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE categoryKey = :key AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultCategoryByKey(key: String): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND isDefault = 0 AND isDeleted = 0 LIMIT 1")
    suspend fun getCustomCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: Category)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM categories WHERE isDefault = 0 AND updatedAt > :since")
    suspend fun getModifiedCustomSince(since: Long): List<Category>

    @Delete
    suspend fun deleteCategory(category: Category)

    suspend fun delete(category: Category) = deleteCategory(category)

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun clearCustomCategories()

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteAllCustomCategories()
}
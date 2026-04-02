package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [Category::class, Expense::class, ExpenseTemplate::class, Loan::class, SavingGoal::class, Stock::class],
    version = 8,           // v7 → v8：categories 表新增 categoryKey 列
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTemplateDao(): ExpenseTemplateDao
    abstract fun loanDao(): LoanDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ===== Migration 6 → 7：云同步字段 =====
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE loans ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loans ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loans ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE saving_goals ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE stocks ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stocks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stocks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        // ===== Migration 7 → 8：类别 key 化（多语言支持）=====
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 新增 categoryKey 列，默认为空字符串
                db.execSQL("ALTER TABLE categories ADD COLUMN categoryKey TEXT NOT NULL DEFAULT ''")

                // 2. 将现有中文默认类别名称映射回 key
                //    自定义类别（isDefault=0）保持 categoryKey='' 不变
                val nameToKey = mapOf(
                    "餐饮" to "food",
                    "交通" to "transport",
                    "购物" to "shopping",
                    "娱乐" to "entertainment",
                    "医疗" to "health",
                    "教育" to "education",
                    "居住" to "housing",
                    "其他" to "other"
                )
                nameToKey.forEach { (name, key) ->
                    db.execSQL(
                        "UPDATE categories SET categoryKey = ? WHERE name = ? AND isDefault = 1",
                        arrayOf(key, name)
                    )
                }
            }
        }

        // ===== 下次 schema 变更参考模板 =====
        // val MIGRATION_8_9 = object : Migration(8, 9) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE expenses ADD COLUMN new_column TEXT")
        //     }
        // }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_app_database"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    insertDefaultCategories(database.categoryDao())
                                    insertDefaultTemplates(
                                        database.expenseTemplateDao(),
                                        database.categoryDao()
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // ===== 插入默认类别（使用 key 字段）=====
        private suspend fun insertDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(name = "food",          categoryKey = "food",          iconName = "restaurant",    color = "#FF5722", isDefault = true),
                Category(name = "transport",     categoryKey = "transport",     iconName = "directions_car", color = "#2196F3", isDefault = true),
                Category(name = "shopping",      categoryKey = "shopping",      iconName = "shopping_cart", color = "#E91E63", isDefault = true),
                Category(name = "entertainment", categoryKey = "entertainment", iconName = "movie",          color = "#9C27B0", isDefault = true),
                Category(name = "health",        categoryKey = "health",        iconName = "local_hospital", color = "#F44336", isDefault = true),
                Category(name = "education",     categoryKey = "education",     iconName = "school",         color = "#00BCD4", isDefault = true),
                Category(name = "housing",       categoryKey = "housing",       iconName = "home",           color = "#FF9800", isDefault = true),
                Category(name = "other",         categoryKey = "other",         iconName = "more_horiz",     color = "#607D8B", isDefault = true),
            )
            defaultCategories.forEach { categoryDao.insertCategory(it) }
        }

        // ===== 插入默认模板（用 categoryKey 查找，更健壮）=====
        private suspend fun insertDefaultTemplates(
            templateDao: ExpenseTemplateDao,
            categoryDao: CategoryDao
        ) {
            val categoryList = categoryDao.getAllCategories().first()

            val transportCategory = categoryList.find { it.categoryKey == "transport" }
            val foodCategory = categoryList.find { it.categoryKey == "food" }
            val entertainmentCategory = categoryList.find { it.categoryKey == "entertainment" }

            val defaultTemplates = mutableListOf<ExpenseTemplate>()

            transportCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(name = "每日通勤", amount = 10.0, categoryId = it.id, note = "地铁/公交")
                )
            }
            foodCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(name = "午餐", amount = 20.0, categoryId = it.id, note = "工作日午餐")
                )
                defaultTemplates.add(
                    ExpenseTemplate(name = "咖啡", amount = 15.0, categoryId = it.id, note = "早晨咖啡")
                )
            }
            entertainmentCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(name = "看电影", amount = 50.0, categoryId = it.id, note = "电影票")
                )
            }

            defaultTemplates.forEach { templateDao.insertTemplate(it) }
        }
    }
}

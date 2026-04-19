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
    entities = [Category::class, Expense::class, ExpenseTemplate::class, Loan::class, SavingGoal::class, Stock::class, MonthlyIncome::class, CheckIn::class, Achievement::class, TokenTransaction::class, AiReport::class],
    version = 13,          // v12 → v13：ai_reports 改为自增主键
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTemplateDao(): ExpenseTemplateDao
    abstract fun loanDao(): LoanDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun stockDao(): StockDao
    abstract fun monthlyIncomeDao(): MonthlyIncomeDao
    abstract fun checkInDao(): CheckInDao
    abstract fun achievementDao(): AchievementDao
    abstract fun tokenTransactionDao(): TokenTransactionDao
    abstract fun aiReportDao(): AiReportDao

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

        // ===== Migration 8 → 9：默认模板名称统一为英文 =====
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE expense_templates
                    SET name = 'Commute', note = 'Subway/Bus'
                    WHERE amount = 10.0
                      AND name IN ('车费', '每日交通', '每日通勤')
                      AND categoryId IN (
                          SELECT id FROM categories WHERE categoryKey = 'transport' AND isDefault = 1
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE expense_templates
                    SET name = 'Lunch', note = 'Weekday lunch'
                    WHERE amount = 20.0
                      AND name = '午餐'
                      AND categoryId IN (
                          SELECT id FROM categories WHERE categoryKey = 'food' AND isDefault = 1
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE expense_templates
                    SET name = 'Coffee', note = 'Morning coffee'
                    WHERE amount = 15.0
                      AND name = '咖啡'
                      AND categoryId IN (
                          SELECT id FROM categories WHERE categoryKey = 'food' AND isDefault = 1
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE expense_templates
                    SET name = 'Movie', note = 'Movie ticket'
                    WHERE amount = 50.0
                      AND name = '看电影'
                      AND categoryId IN (
                          SELECT id FROM categories WHERE categoryKey = 'entertainment' AND isDefault = 1
                      )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS monthly_income (
                        yearMonth TEXT NOT NULL PRIMARY KEY,
                        amount REAL NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        firestoreId TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS check_ins (
                        date TEXT NOT NULL PRIMARY KEY,
                        tokensEarned INTEGER NOT NULL DEFAULT 0,
                        firestoreId TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievements (
                        achievementId TEXT NOT NULL PRIMARY KEY,
                        unlockedAt INTEGER NOT NULL DEFAULT 0,
                        tokensAwarded INTEGER NOT NULL DEFAULT 0,
                        firestoreId TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS token_transactions (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        firestoreId TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_reports (
                        yearMonth TEXT NOT NULL PRIMARY KEY,
                        content TEXT NOT NULL,
                        generatedAt INTEGER NOT NULL,
                        firestoreId TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE ai_reports_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, yearMonth TEXT NOT NULL, content TEXT NOT NULL, generatedAt TEXT NOT NULL, firestoreId TEXT NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO ai_reports_new (yearMonth, content, generatedAt, firestoreId, updatedAt, isDeleted) SELECT yearMonth, content, generatedAt, firestoreId, updatedAt, isDeleted FROM ai_reports")
                db.execSQL("DROP TABLE ai_reports")
                db.execSQL("ALTER TABLE ai_reports_new RENAME TO ai_reports")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_app_database"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
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

            val foodCategory = categoryList.find { it.categoryKey == "food" }
            val entertainmentCategory = categoryList.find { it.categoryKey == "entertainment" }

            val defaultTemplates = mutableListOf<ExpenseTemplate>()

            foodCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(name = "Lunch", amount = 20.0, categoryId = it.id, note = "Weekday lunch")
                )
                defaultTemplates.add(
                    ExpenseTemplate(name = "Coffee", amount = 15.0, categoryId = it.id, note = "Morning coffee")
                )
            }
            entertainmentCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(name = "Movie", amount = 50.0, categoryId = it.id, note = "Movie ticket")
                )
            }

            defaultTemplates.forEach { templateDao.insertTemplate(it) }
        }
    }
}

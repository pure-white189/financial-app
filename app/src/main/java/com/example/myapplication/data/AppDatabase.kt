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
    version = 7,
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




        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_app_database"

                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .addMigrations(MIGRATION_6_7)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    // 插入默认类别
                                    insertDefaultCategories(database.categoryDao())
                                    // 插入默认模板
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

        // 插入默认类别
        private suspend fun insertDefaultCategories(categoryDao: CategoryDao) {
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

            defaultCategories.forEach { category ->
                categoryDao.insertCategory(category)
            }
        }

        // ===== 新增：插入默认模板 =====
        private suspend fun insertDefaultTemplates(
            templateDao: ExpenseTemplateDao,
            categoryDao: CategoryDao
        ) {
            // 获取所有类别的 Flow
            val categoriesFlow = categoryDao.getAllCategories()

            // 使用 first() 获取第一次发射的值
            val categoryList = categoriesFlow.first()

            // 根据类别创建默认模板
            val transportCategory = categoryList.find { it.name == "交通" }
            val foodCategory = categoryList.find { it.name == "餐饮" }
            val entertainmentCategory = categoryList.find { it.name == "娱乐" }

            val defaultTemplates = mutableListOf<ExpenseTemplate>()

            // 交通模板
            transportCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(
                        name = "每日通勤",
                        amount = 10.0,
                        categoryId = it.id,
                        note = "地铁/公交"
                    )
                )
            }

            // 餐饮模板
            foodCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(
                        name = "午餐",
                        amount = 20.0,
                        categoryId = it.id,
                        note = "工作日午餐"
                    )
                )
                defaultTemplates.add(
                    ExpenseTemplate(
                        name = "咖啡",
                        amount = 15.0,
                        categoryId = it.id,
                        note = "早晨咖啡"
                    )
                )
            }

            // 娱乐模板
            entertainmentCategory?.let {
                defaultTemplates.add(
                    ExpenseTemplate(
                        name = "看电影",
                        amount = 50.0,
                        categoryId = it.id,
                        note = "电影票"
                    )
                )
            }

            // 插入所有默认模板
            defaultTemplates.forEach { template ->
                templateDao.insertTemplate(template)
            }
        }
    }
}
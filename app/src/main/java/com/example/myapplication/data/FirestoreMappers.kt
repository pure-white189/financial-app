package com.example.myapplication.data

import com.google.firebase.firestore.DocumentSnapshot

// ── Expense ───────────────────────────────────────
fun Expense.toMap(): Map<String, Any?> = mapOf(
    "firestoreId" to firestoreId,
    "amount"      to amount,
    "originalAmount" to originalAmount,
    "originalCurrency" to originalCurrency,
    "exchangeRate" to exchangeRate,
    "categoryId"  to categoryId,
    "date"        to date,
    "note"        to note,
    "createdAt"   to createdAt,
    "updatedAt"   to updatedAt,
    "isDeleted"   to isDeleted
)

fun DocumentSnapshot.toExpense(): Expense = Expense(
    firestoreId = id,
    amount      = getDouble("amount") ?: 0.0,
    originalAmount = getDouble("originalAmount"),
    originalCurrency = getString("originalCurrency"),
    exchangeRate = getDouble("exchangeRate"),
    categoryId  = getLong("categoryId")?.toInt() ?: 0,
    date        = getLong("date") ?: 0L,
    note        = getString("note") ?: "",
    createdAt   = getLong("createdAt") ?: 0L,
    updatedAt   = getLong("updatedAt") ?: 0L,
    isDeleted   = getBoolean("isDeleted") ?: false
)

// ── Loan ──────────────────────────────────────────
fun Loan.toMap(): Map<String, Any?> = mapOf(
    "firestoreId" to firestoreId,
    "type"        to type,
    "personName"  to personName,
    "amount"      to amount,
    "originalAmount" to originalAmount,
    "originalCurrency" to originalCurrency,
    "exchangeRate" to exchangeRate,
    "date"        to date,
    "dueDate"     to dueDate,
    "note"        to note,
    "isRepaid"    to isRepaid,
    "createdAt"   to createdAt,
    "updatedAt"   to updatedAt,
    "isDeleted"   to isDeleted
)

fun DocumentSnapshot.toLoan(): Loan = Loan(
    firestoreId = id,
    type        = getString("type") ?: "",
    personName  = getString("personName") ?: "",
    amount      = getDouble("amount") ?: 0.0,
    originalAmount = getDouble("originalAmount"),
    originalCurrency = getString("originalCurrency"),
    exchangeRate = getDouble("exchangeRate"),
    date        = getLong("date") ?: 0L,
    dueDate     = getLong("dueDate"),
    note        = getString("note") ?: "",
    isRepaid    = getBoolean("isRepaid") ?: false,
    createdAt   = getLong("createdAt") ?: 0L,
    updatedAt   = getLong("updatedAt") ?: 0L,
    isDeleted   = getBoolean("isDeleted") ?: false
)

// ── SavingGoal ────────────────────────────────────
fun SavingGoal.toMap(): Map<String, Any?> = mapOf(
    "firestoreId"   to firestoreId,
    "name"          to name,
    "targetAmount"  to targetAmount,
    "currentAmount" to currentAmount,
    "deadline"      to deadline,
    "note"          to note,
    "isCompleted"   to isCompleted,
    "createdAt"     to createdAt,
    "updatedAt"     to updatedAt,
    "isDeleted"     to isDeleted
)

fun DocumentSnapshot.toSavingGoal(): SavingGoal = SavingGoal(
    firestoreId   = id,
    name          = getString("name") ?: "",
    targetAmount  = getDouble("targetAmount") ?: 0.0,
    currentAmount = getDouble("currentAmount") ?: 0.0,
    deadline      = getLong("deadline"),
    note          = getString("note") ?: "",
    isCompleted   = getBoolean("isCompleted") ?: false,
    createdAt     = getLong("createdAt") ?: 0L,
    updatedAt     = getLong("updatedAt") ?: 0L,
    isDeleted     = getBoolean("isDeleted") ?: false
)

// ── Stock ─────────────────────────────────────────
fun Stock.toMap(): Map<String, Any?> = mapOf(
    "firestoreId"  to firestoreId,
    "symbol"       to symbol,
    "name"         to name,
    "shares"       to shares,
    "costPrice"    to costPrice,
    "currentPrice" to currentPrice,
    "currency"     to currency,
    "lastUpdated"  to lastUpdated,
    "market"       to market,
    "createdAt"    to createdAt,
    "updatedAt"    to updatedAt,
    "isDeleted"    to isDeleted
)

fun DocumentSnapshot.toStock(): Stock = Stock(
    firestoreId  = id,
    symbol       = getString("symbol") ?: "",
    name         = getString("name") ?: "",
    shares       = getDouble("shares") ?: 0.0,
    costPrice    = getDouble("costPrice") ?: 0.0,
    currentPrice = getDouble("currentPrice") ?: 0.0,
    currency     = getString("currency") ?: "HKD",
    lastUpdated  = getLong("lastUpdated") ?: 0L,
    market       = getString("market") ?: "HK",
    createdAt    = getLong("createdAt") ?: 0L,
    updatedAt    = getLong("updatedAt") ?: 0L,
    isDeleted    = getBoolean("isDeleted") ?: false
)

// ── MonthlyIncome ───────────────────────────────
fun MonthlyIncome.toFirestoreMap(): Map<String, Any> = mapOf(
    "yearMonth" to yearMonth,
    "amount" to amount,
    "note" to note,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

fun Map<String, Any>.toMonthlyIncome(firestoreId: String): MonthlyIncome = MonthlyIncome(
    yearMonth = this["yearMonth"] as String,
    amount = (this["amount"] as Number).toDouble(),
    note = (this["note"] as? String) ?: "",
    firestoreId = firestoreId,
    updatedAt = (this["updatedAt"] as Number).toLong(),
    isDeleted = (this["isDeleted"] as? Boolean) ?: false
)

// ── CheckIn ──────────────────────────────────────
fun CheckIn.toFirestoreMap(): Map<String, Any> = mapOf(
    "date"         to date,
    "tokensEarned" to tokensEarned,
    "updatedAt"    to updatedAt,
    "isDeleted"    to isDeleted
)

fun Map<String, Any>.toCheckIn(firestoreId: String): CheckIn = CheckIn(
    date         = this["date"] as String,
    tokensEarned = (this["tokensEarned"] as? Number)?.toInt() ?: 0,
    firestoreId  = firestoreId,
    updatedAt    = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
    isDeleted    = (this["isDeleted"] as? Number)?.toInt() ?: 0
)

// ── Achievement ───────────────────────────────────
fun Achievement.toFirestoreMap(): Map<String, Any> = mapOf(
    "achievementId" to achievementId,
    "unlockedAt"    to unlockedAt,
    "tokensAwarded" to tokensAwarded,
    "updatedAt"     to updatedAt,
    "isDeleted"     to isDeleted
)

fun Map<String, Any>.toAchievement(firestoreId: String): Achievement = Achievement(
    achievementId = this["achievementId"] as String,
    unlockedAt    = (this["unlockedAt"] as? Number)?.toLong() ?: 0L,
    tokensAwarded = (this["tokensAwarded"] as? Number)?.toInt() ?: 0,
    firestoreId   = firestoreId,
    updatedAt     = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
    isDeleted     = (this["isDeleted"] as? Number)?.toInt() ?: 0
)

// ── TokenTransaction ──────────────────────────────
fun TokenTransaction.toFirestoreMap(): Map<String, Any> = mapOf(
    "type"        to type,
    "amount"      to amount,
    "description" to description,
    "timestamp"   to timestamp,
    "updatedAt"   to updatedAt,
    "isDeleted"   to isDeleted
)

fun Map<String, Any>.toTokenTransaction(firestoreId: String): TokenTransaction = TokenTransaction(
    type        = this["type"] as String,
    amount      = (this["amount"] as? Number)?.toInt() ?: 0,
    description = (this["description"] as? String) ?: "",
    timestamp   = (this["timestamp"] as? Number)?.toLong() ?: 0L,
    firestoreId = firestoreId,
    updatedAt   = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
    isDeleted   = (this["isDeleted"] as? Number)?.toInt() ?: 0
)

// ── AiReport ──────────────────────────────────────────────
fun AiReport.toFirestoreMap(): Map<String, Any> = mapOf(
    "yearMonth"   to yearMonth,
    "content"     to content,
    "generatedAt" to generatedAt,
    "updatedAt"   to updatedAt,
    "isDeleted"   to isDeleted
)

fun Map<String, Any>.toAiReport(firestoreId: String): AiReport = AiReport(
    id          = 0,
    yearMonth   = (this["yearMonth"] as? String) ?: "",
    content     = (this["content"] as? String) ?: "",
    generatedAt = (this["generatedAt"] as? String)
        ?: ((this["generatedAt"] as? Number)?.toLong()?.toString() ?: ""),
    firestoreId = firestoreId,
    updatedAt   = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
    isDeleted   = (this["isDeleted"] as? Number)?.toInt() ?: 0
)

// ─── Category ────────────────────────────────────────────────

fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "categoryKey" to categoryKey,
    "iconPath" to iconPath,
    "iconName" to iconName,
    "color" to color,
    "isDefault" to isDefault,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

fun Map<String, Any?>.toCategory(firestoreId: String): Category = Category(
    firestoreId = firestoreId,
    name = this["name"] as? String ?: "",
    categoryKey = this["categoryKey"] as? String ?: "",
    iconPath = this["iconPath"] as? String,
    iconName = this["iconName"] as? String,
    color = this["color"] as? String ?: "#FF5722",
    isDefault = this["isDefault"] as? Boolean ?: false,
    updatedAt = this["updatedAt"] as? Long ?: 0L,
    isDeleted = this["isDeleted"] as? Boolean ?: false
)

// ─── ExpenseTemplate ─────────────────────────────────────────

fun ExpenseTemplate.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "amount" to amount,
    "categoryKey" to categoryKey,
    "note" to note,
    "isPinned" to isPinned,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

fun Map<String, Any?>.toExpenseTemplate(firestoreId: String, categoryId: Int): ExpenseTemplate = ExpenseTemplate(
    firestoreId = firestoreId,
    name = this["name"] as? String ?: "",
    amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
    categoryId = categoryId,
    categoryKey = this["categoryKey"] as? String ?: "",
    note = this["note"] as? String ?: "",
    isPinned = this["isPinned"] as? Boolean ?: false,
    createdAt = (this["createdAt"] as? Long) ?: System.currentTimeMillis(),
    updatedAt = (this["updatedAt"] as? Long) ?: 0L,
    isDeleted = this["isDeleted"] as? Boolean ?: false
)


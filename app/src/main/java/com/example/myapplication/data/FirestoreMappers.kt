package com.example.myapplication.data

import com.google.firebase.firestore.DocumentSnapshot

// ── Expense ───────────────────────────────────────
fun Expense.toMap(): Map<String, Any?> = mapOf(
    "firestoreId" to firestoreId,
    "amount"      to amount,
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
    lastUpdated  = getLong("lastUpdated") ?: 0L,
    market       = getString("market") ?: "HK",
    createdAt    = getLong("createdAt") ?: 0L,
    updatedAt    = getLong("updatedAt") ?: 0L,
    isDeleted    = getBoolean("isDeleted") ?: false
)
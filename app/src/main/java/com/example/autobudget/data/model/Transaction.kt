package com.example.autobudget.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val merchantName: String,
    val transactionType: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val category: String = ""
)

enum class TransactionType {
    CREDIT,
    DEBIT,
    UNKNOWN
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}


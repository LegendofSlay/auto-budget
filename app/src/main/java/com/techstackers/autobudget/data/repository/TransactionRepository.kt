package com.techstackers.autobudget.data.repository

import com.techstackers.autobudget.data.local.TransactionDao
import com.techstackers.autobudget.data.model.SyncStatus
import com.techstackers.autobudget.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit)
    }

    val pendingTransactions: Flow<List<Transaction>> =
        transactionDao.getPendingTransactionsFlow(SyncStatus.PENDING)

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun updateSyncStatus(id: Long, status: SyncStatus) {
        transactionDao.updateSyncStatus(id, status)
    }

    suspend fun getPendingTransactions(): List<Transaction> {
        return transactionDao.getTransactionsByStatus(SyncStatus.PENDING)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
}


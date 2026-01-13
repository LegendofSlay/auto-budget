package com.example.autobudget.sheets

import android.content.Context
import android.util.Log
import com.example.autobudget.data.local.PreferencesManager
import com.example.autobudget.data.model.SyncStatus
import com.example.autobudget.data.model.Transaction
import com.example.autobudget.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SyncManager(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val sheetsManager: GoogleSheetsManager,
    private val preferencesManager: PreferencesManager
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Syncs a single transaction to Google Sheets
     */
    suspend fun syncTransaction(transaction: Transaction): Boolean {
        val spreadsheetId = preferencesManager.spreadsheetId.first()
        val sheetTabName = preferencesManager.sheetTabName.first()

        if (spreadsheetId.isNullOrBlank()) {
            Log.w(TAG, "No spreadsheet configured, skipping sync")
            return false
        }

        return try {
            val result = sheetsManager.appendTransaction(spreadsheetId, transaction, sheetTabName)

            if (result.isSuccess) {
                transactionRepository.updateSyncStatus(transaction.id, SyncStatus.SYNCED)
                Log.d(TAG, "Transaction synced: ${transaction.id}")
                true
            } else {
                transactionRepository.updateSyncStatus(transaction.id, SyncStatus.FAILED)
                Log.e(TAG, "Failed to sync transaction: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            transactionRepository.updateSyncStatus(transaction.id, SyncStatus.FAILED)
            Log.e(TAG, "Error syncing transaction", e)
            false
        }
    }

    /**
     * Syncs all pending transactions
     */
    suspend fun syncPendingTransactions(): SyncResult {
        val spreadsheetId = preferencesManager.spreadsheetId.first()

        if (spreadsheetId.isNullOrBlank()) {
            return SyncResult(0, 0, "No spreadsheet configured")
        }

        val pendingTransactions = transactionRepository.getPendingTransactions()

        if (pendingTransactions.isEmpty()) {
            return SyncResult(0, 0, "No pending transactions")
        }

        var successCount = 0
        var failureCount = 0

        for (transaction in pendingTransactions) {
            if (syncTransaction(transaction)) {
                successCount++
            } else {
                failureCount++
            }
        }

        return SyncResult(successCount, failureCount)
    }

    /**
     * Start background sync process
     */
    fun startBackgroundSync() {
        syncScope.launch {
            syncPendingTransactions()
        }
    }
}

data class SyncResult(
    val successCount: Int,
    val failureCount: Int,
    val message: String? = null
)


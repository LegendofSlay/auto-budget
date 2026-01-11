package com.example.autobudget.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.autobudget.data.local.PreferencesManager
import com.example.autobudget.data.local.TransactionDatabase
import com.example.autobudget.data.repository.TransactionRepository
import com.example.autobudget.sheets.GoogleSheetsManager
import com.example.autobudget.sheets.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var transactionParser: TransactionParser
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var syncManager: SyncManager
    private lateinit var notificationHelper: SyncNotificationHelper
    private lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "TransactionNotifListener"

        // Track if the service is connected
        var isConnected: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        transactionParser = TransactionParser()

        val database = TransactionDatabase.getDatabase(applicationContext)
        transactionRepository = TransactionRepository(database.transactionDao())

        val sheetsManager = GoogleSheetsManager(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        syncManager = SyncManager(
            applicationContext,
            transactionRepository,
            sheetsManager,
            preferencesManager
        )

        notificationHelper = SyncNotificationHelper(applicationContext)

        // Load configured financial apps from preferences
        serviceScope.launch {
            preferencesManager.financialApps.collect { apps ->
                transactionParser.updateConfiguredApps(apps)
                Log.d(TAG, "Updated configured apps: ${apps.size} custom apps")
            }
        }

        // Load excluded apps from preferences
        serviceScope.launch {
            preferencesManager.excludedApps.collect { apps ->
                transactionParser.updateExcludedApps(apps)
                Log.d(TAG, "Updated excluded apps: ${apps.size} excluded apps")
            }
        }

        // Load custom category mappings from preferences
        serviceScope.launch {
            preferencesManager.categoryMappings.collect { mappings ->
                transactionParser.updateCategoryMappings(mappings)
                Log.d(TAG, "Updated category mappings: ${mappings.size} custom mappings")
            }
        }

        Log.d(TAG, "Service created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName

        // Check if this is from a financial app
        if (!transactionParser.isFinancialApp(packageName)) {
            return
        }

        Log.d(TAG, "Financial notification from: $packageName")

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()

        // Try to parse the notification
        val transaction = transactionParser.parseNotification(
            title = title,
            text = bigText ?: text,
            packageName = packageName
        )

        if (transaction != null) {
            Log.d(TAG, "Parsed transaction: $${transaction.amount} at ${transaction.merchantName}")

            // Save to database and sync to Google Sheets
            serviceScope.launch {
                try {
                    // Insert transaction into database
                    val transactionId = transactionRepository.insertTransaction(transaction)
                    Log.d(TAG, "Transaction saved to database with ID: $transactionId")

                    // Create a copy with the generated ID for syncing
                    val transactionWithId = transaction.copy(id = transactionId)

                    // Sync to Google Sheets
                    val syncSuccess = syncManager.syncTransaction(transactionWithId)

                    if (syncSuccess) {
                        Log.d(TAG, "Transaction synced to Google Sheets")

                        // Show success notification
                        notificationHelper.showSyncSuccessNotification(
                            transaction.merchantName,
                            transaction.amount,
                            transaction.category
                        )

                        // Remove the notification only after successful sync
                        cancelNotification(sbn.key)
                        Log.d(TAG, "Notification dismissed")
                    } else {
                        Log.w(TAG, "Failed to sync transaction to Google Sheets, notification not dismissed")

                        // Show failure notification
                        notificationHelper.showSyncFailureNotification(
                            transaction.merchantName,
                            transaction.amount,
                            transaction.category,
                            "Check Google account connection and spreadsheet configuration"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save transaction", e)

                    // Show failure notification with error details
                    notificationHelper.showSyncFailureNotification(
                        transaction.merchantName,
                        transaction.amount,
                        transaction.category,
                        e.message
                    )
                }
            }
        } else {
            Log.d(TAG, "Could not parse transaction from notification: $title - $text")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We don't need to handle removed notifications
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}


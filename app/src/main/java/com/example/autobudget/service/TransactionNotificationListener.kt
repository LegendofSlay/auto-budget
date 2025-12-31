package com.example.autobudget.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.autobudget.data.local.TransactionDatabase
import com.example.autobudget.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var transactionParser: TransactionParser
    private lateinit var transactionRepository: TransactionRepository

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

            // Save to database
            serviceScope.launch {
                try {
                    transactionRepository.insertTransaction(transaction)
                    Log.d(TAG, "Transaction saved to database")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save transaction", e)
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


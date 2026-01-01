package com.example.autobudget.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

class SyncNotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "transaction_sync_channel"
        private const val CHANNEL_NAME = "Transaction Sync"
        private const val CHANNEL_DESCRIPTION = "Notifications for transaction sync status"
        private var notificationId = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Show a notification for successful transaction sync
     */
    fun showSyncSuccessNotification(merchantName: String, amount: Double) {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon
            .setContentTitle("Transaction Synced ✓")
            .setContentText("$merchantName - $$formattedAmount")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Successfully synced to Google Sheets\n$merchantName - $$formattedAmount"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(getNextNotificationId(), notification)
        } catch (e: SecurityException) {
            // User hasn't granted notification permission
            android.util.Log.w("SyncNotificationHelper", "Notification permission not granted", e)
        }
    }

    /**
     * Show a notification for failed transaction sync
     */
    fun showSyncFailureNotification(merchantName: String, amount: Double, errorMessage: String? = null) {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val contentText = "$merchantName - $$formattedAmount"
        val bigText = if (errorMessage != null) {
            "Failed to sync to Google Sheets\n$contentText\nError: $errorMessage"
        } else {
            "Failed to sync to Google Sheets\n$contentText"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // TODO: Use app icon
            .setContentTitle("Sync Failed ✗")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(getNextNotificationId(), notification)
        } catch (e: SecurityException) {
            // User hasn't granted notification permission
            android.util.Log.w("SyncNotificationHelper", "Notification permission not granted", e)
        }
    }

    /**
     * Show a notification for pending sync (saved locally but not synced yet)
     */
    fun showSyncPendingNotification(merchantName: String, amount: Double) {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transaction Saved")
            .setContentText("$merchantName - $$formattedAmount")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Saved locally, sync pending\n$merchantName - $$formattedAmount"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(getNextNotificationId(), notification)
        } catch (e: SecurityException) {
            // User hasn't granted notification permission
            android.util.Log.w("SyncNotificationHelper", "Notification permission not granted", e)
        }
    }

    private fun getNextNotificationId(): Int {
        return notificationId++
    }
}


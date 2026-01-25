package com.techstackers.autobudget

import android.app.Application
import com.techstackers.autobudget.billing.BillingManager
import com.techstackers.autobudget.data.local.PreferencesManager
import com.techstackers.autobudget.data.local.SubscriptionManager
import com.techstackers.autobudget.data.local.TransactionDatabase
import com.techstackers.autobudget.data.repository.TransactionRepository
import com.techstackers.autobudget.sheets.GoogleAuthManager
import com.techstackers.autobudget.sheets.GoogleSheetsManager
import com.techstackers.autobudget.sheets.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AutoBudgetApplication : Application() {

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Database
    val database by lazy { TransactionDatabase.getDatabase(this) }

    // Repository
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }

    // Preferences
    val preferencesManager by lazy { PreferencesManager(this) }

    // Billing
    val billingManager by lazy { BillingManager(this, applicationScope) }

    // Subscription
    val subscriptionManager by lazy { SubscriptionManager(this, billingManager, applicationScope) }

    // Google Services
    val googleAuthManager by lazy { GoogleAuthManager(this) }
    val googleSheetsManager by lazy { GoogleSheetsManager(this) }

    // Sync Manager
    val syncManager by lazy {
        SyncManager(
            this,
            transactionRepository,
            googleSheetsManager,
            preferencesManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AutoBudgetApplication
            private set
    }
}


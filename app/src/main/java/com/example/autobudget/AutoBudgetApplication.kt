package com.example.autobudget

import android.app.Application
import com.example.autobudget.data.local.PreferencesManager
import com.example.autobudget.data.local.TransactionDatabase
import com.example.autobudget.data.repository.TransactionRepository
import com.example.autobudget.sheets.GoogleAuthManager
import com.example.autobudget.sheets.GoogleSheetsManager
import com.example.autobudget.sheets.SyncManager

class AutoBudgetApplication : Application() {

    // Database
    val database by lazy { TransactionDatabase.getDatabase(this) }

    // Repository
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }

    // Preferences
    val preferencesManager by lazy { PreferencesManager(this) }

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


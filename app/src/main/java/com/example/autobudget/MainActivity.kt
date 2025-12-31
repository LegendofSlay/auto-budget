package com.example.autobudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autobudget.ui.screens.HomeScreen
import com.example.autobudget.ui.theme.AutoBudgetTheme
import com.example.autobudget.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AutoBudgetApplication

        setContent {
            AutoBudgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.Factory(
                            transactionRepository = app.transactionRepository,
                            preferencesManager = app.preferencesManager,
                            googleAuthManager = app.googleAuthManager,
                            googleSheetsManager = app.googleSheetsManager,
                            syncManager = app.syncManager
                        )
                    )

                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger sync when app resumes
        (application as AutoBudgetApplication).syncManager.startBackgroundSync()
    }
}
package com.example.autobudget.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.autobudget.data.local.PreferencesManager
import com.example.autobudget.data.model.Transaction
import com.example.autobudget.data.repository.TransactionRepository
import com.example.autobudget.sheets.GoogleAuthManager
import com.example.autobudget.sheets.GoogleSheetsManager
import com.example.autobudget.sheets.GoogleSignInState
import com.example.autobudget.sheets.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager,
    private val googleAuthManager: GoogleAuthManager,
    private val googleSheetsManager: GoogleSheetsManager,
    private val syncManager: SyncManager
) : ViewModel() {

    // Transactions
    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository
        .getRecentTransactions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTransactions: StateFlow<List<Transaction>> = transactionRepository
        .pendingTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Google Sign-In State
    val signInState: StateFlow<GoogleSignInState> = googleAuthManager.signInState

    // Spreadsheet Config
    private val _spreadsheetId = MutableStateFlow<String?>(null)
    val spreadsheetId: StateFlow<String?> = _spreadsheetId.asStateFlow()

    private val _spreadsheetName = MutableStateFlow<String?>(null)
    val spreadsheetName: StateFlow<String?> = _spreadsheetName.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Notification Listener Status
    private val _notificationListenerEnabled = MutableStateFlow(false)
    val notificationListenerEnabled: StateFlow<Boolean> = _notificationListenerEnabled.asStateFlow()

    // Financial Apps Configuration
    val configuredFinancialApps: StateFlow<Set<String>> = preferencesManager.financialApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val excludedApps: StateFlow<Set<String>> = preferencesManager.excludedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val categoryMappings: StateFlow<Map<String, String>> = preferencesManager.categoryMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categories: StateFlow<Set<String>> = preferencesManager.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // Load saved preferences
        viewModelScope.launch {
            preferencesManager.spreadsheetId.collect { id ->
                _spreadsheetId.value = id
            }
        }
        viewModelScope.launch {
            preferencesManager.spreadsheetName.collect { name ->
                _spreadsheetName.value = name
            }
        }
    }

    fun checkNotificationListenerStatus(context: Context) {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        _notificationListenerEnabled.value = enabledPackages.contains(context.packageName)
    }

    fun getSignInIntent(): Intent {
        return googleAuthManager.getSignInIntent()
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = googleAuthManager.handleSignInResult(data)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthManager.signOut()
            // Only clear Google account and spreadsheet data, keep app configurations
            preferencesManager.clearGoogleAccount()
            preferencesManager.clearSpreadsheetConfig()
            _spreadsheetId.value = null
            _spreadsheetName.value = null
        }
    }

    fun setSpreadsheet(urlOrId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val spreadsheetId = googleSheetsManager.extractSpreadsheetId(urlOrId)

            if (spreadsheetId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid spreadsheet URL or ID"
                )
                return@launch
            }

            val result = googleSheetsManager.validateSpreadsheet(spreadsheetId)

            result.fold(
                onSuccess = { title ->
                    preferencesManager.saveSpreadsheetConfig(spreadsheetId, title)
                    _spreadsheetId.value = spreadsheetId
                    _spreadsheetName.value = title

                    // Try to ensure headers exist
                    googleSheetsManager.ensureHeadersExist(spreadsheetId)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Connected to: $title"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Could not access spreadsheet: ${error.message}"
                    )
                }
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = syncManager.syncPendingTransactions()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = if (result.successCount > 0)
                    "Synced ${result.successCount} transactions"
                else null,
                errorMessage = result.message ?: if (result.failureCount > 0)
                    "Failed to sync ${result.failureCount} transactions"
                else null
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun addFinancialApp(packageName: String) {
        viewModelScope.launch {
            preferencesManager.addFinancialApp(packageName)
            _uiState.value = _uiState.value.copy(
                successMessage = "Added $packageName to monitored apps"
            )
        }
    }

    fun removeFinancialApp(packageName: String) {
        viewModelScope.launch {
            preferencesManager.removeFinancialApp(packageName)
            _uiState.value = _uiState.value.copy(
                successMessage = "Removed $packageName from monitored apps"
            )
        }
    }

    fun addExcludedApp(packageName: String) {
        viewModelScope.launch {
            preferencesManager.addExcludedApp(packageName)
            _uiState.value = _uiState.value.copy(
                successMessage = "Added $packageName to exclusion list"
            )
        }
    }

    fun removeExcludedApp(packageName: String) {
        viewModelScope.launch {
            preferencesManager.removeExcludedApp(packageName)
            _uiState.value = _uiState.value.copy(
                successMessage = "Removed $packageName from exclusion list"
            )
        }
    }

    fun addCategoryMapping(keyword: String, category: String) {
        viewModelScope.launch {
            preferencesManager.addCategoryMapping(keyword, category)
            _uiState.value = _uiState.value.copy(
                successMessage = "Added category mapping: $keyword → $category"
            )
        }
    }

    fun removeCategoryMapping(keyword: String) {
        viewModelScope.launch {
            preferencesManager.removeCategoryMapping(keyword)
            _uiState.value = _uiState.value.copy(
                successMessage = "Removed category mapping for: $keyword"
            )
        }
    }

    fun addCategory(category: String) {
        viewModelScope.launch {
            preferencesManager.addCategory(category)
            _uiState.value = _uiState.value.copy(
                successMessage = "Added category: $category"
            )
        }
    }

    fun removeCategory(category: String) {
        viewModelScope.launch {
            preferencesManager.removeCategory(category)
            _uiState.value = _uiState.value.copy(
                successMessage = "Removed category: $category"
            )
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val preferencesManager: PreferencesManager,
        private val googleAuthManager: GoogleAuthManager,
        private val googleSheetsManager: GoogleSheetsManager,
        private val syncManager: SyncManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                transactionRepository,
                preferencesManager,
                googleAuthManager,
                googleSheetsManager,
                syncManager
            ) as T
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)


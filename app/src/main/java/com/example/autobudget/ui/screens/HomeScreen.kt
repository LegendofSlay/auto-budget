package com.example.autobudget.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.autobudget.ui.components.ConnectionStatusCard
import com.example.autobudget.ui.components.TransactionCard
import com.example.autobudget.ui.viewmodel.HomeViewModel
import com.example.autobudget.service.TransactionParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val signInState by viewModel.signInState.collectAsState()
    val spreadsheetName by viewModel.spreadsheetName.collectAsState()
    val notificationListenerEnabled by viewModel.notificationListenerEnabled.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val configuredFinancialApps by viewModel.configuredFinancialApps.collectAsState()
    val excludedApps by viewModel.excludedApps.collectAsState()
    val categoryMappings by viewModel.categoryMappings.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showSpreadsheetDialog by remember { mutableStateOf(false) }
    var showConfigureAppsScreen by remember { mutableStateOf(false) }
    var showConfigureCategoryScreen by remember { mutableStateOf(false) }
    var showSettingsDropdown by remember { mutableStateOf(false) }

    // Show ConfigureAppsScreen if requested
    if (showConfigureAppsScreen) {
        ConfigureAppsScreen(
            defaultApps = TransactionParser.DEFAULT_FINANCIAL_APPS,
            configuredApps = configuredFinancialApps,
            excludedApps = excludedApps,
            onBack = { showConfigureAppsScreen = false },
            onAddApp = { packageName ->
                viewModel.addFinancialApp(packageName)
            },
            onRemoveApp = { packageName ->
                viewModel.removeFinancialApp(packageName)
            },
            onAddExcludedApp = { packageName ->
                viewModel.addExcludedApp(packageName)
            },
            onRemoveExcludedApp = { packageName ->
                viewModel.removeExcludedApp(packageName)
            }
        )
        return
    }

    // Show ConfigureCategoryScreen if requested
    if (showConfigureCategoryScreen) {
        ConfigureCategoryScreen(
            defaultMappings = TransactionParser.DEFAULT_CATEGORY_KEYWORDS,
            customMappings = categoryMappings,
            categories = categories,
            onBack = { showConfigureCategoryScreen = false },
            onAddMapping = { keyword, category ->
                viewModel.addCategoryMapping(keyword, category)
            },
            onRemoveMapping = { keyword ->
                viewModel.removeCategoryMapping(keyword)
            },
            onAddCategory = { category ->
                viewModel.addCategory(category)
            },
            onRemoveCategory = { category ->
                viewModel.removeCategory(category)
            }
        )
        return
    }

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    // Notification settings launcher
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Refresh notification listener status when user returns from settings
        viewModel.checkNotificationListenerStatus(context)
    }

    // Observe lifecycle to refresh when app comes back into view
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh notification listener status when app resumes
                viewModel.checkNotificationListenerStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Check notification listener status on initial composition
    LaunchedEffect(Unit) {
        viewModel.checkNotificationListenerStatus(context)
    }

    // Show snackbar for messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AutoBudget",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showSettingsDropdown = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = showSettingsDropdown,
                            onDismissRequest = { showSettingsDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Configure Spreadsheet") },
                                onClick = {
                                    showSettingsDropdown = false
                                    showSpreadsheetDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Configure Detected Apps") },
                                onClick = {
                                    showSettingsDropdown = false
                                    showConfigureAppsScreen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Configure Category Mapping") },
                                onClick = {
                                    showSettingsDropdown = false
                                    showConfigureCategoryScreen = true
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (pendingTransactions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.syncNow() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status Card
                item {
                    ConnectionStatusCard(
                        signInState = signInState,
                        spreadsheetName = spreadsheetName,
                        notificationListenerEnabled = notificationListenerEnabled,
                        pendingCount = pendingTransactions.size,
                        onSignInClick = {
                            signInLauncher.launch(viewModel.getSignInIntent())
                        },
                        onSignOutClick = { viewModel.signOut() },
                        onSetupNotificationsClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            notificationSettingsLauncher.launch(intent)
                        },
                        onSyncClick = { viewModel.syncNow() }
                    )
                }

                // Recent Transactions Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Transaction List
                if (recentTransactions.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions yet. Financial notifications will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(recentTransactions) { transaction ->
                        TransactionCard(transaction = transaction)
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Spreadsheet Configuration Dialog
    if (showSpreadsheetDialog) {
        SpreadsheetConfigDialog(
            currentSpreadsheetName = spreadsheetName,
            onDismiss = { showSpreadsheetDialog = false },
            onConfirm = { urlOrId ->
                viewModel.setSpreadsheet(urlOrId)
                showSpreadsheetDialog = false
            }
        )
    }
}

@Composable
fun SpreadsheetConfigDialog(
    currentSpreadsheetName: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var spreadsheetInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Google Sheet") },
        text = {
            Column {
                if (currentSpreadsheetName != null) {
                    Text(
                        text = "Current: $currentSpreadsheetName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Enter the Google Sheets URL or Spreadsheet ID:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = spreadsheetInput,
                    onValueChange = { spreadsheetInput = it },
                    label = { Text("Spreadsheet URL or ID") },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Note: Make sure the spreadsheet has a sheet tab named 'Transactions'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(spreadsheetInput) },
                enabled = spreadsheetInput.isNotBlank()
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


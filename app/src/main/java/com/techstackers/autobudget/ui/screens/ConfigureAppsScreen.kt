package com.techstackers.autobudget.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// Data class to hold installed app information
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

// Helper function to get installed apps
suspend fun getInstalledApps(packageManager: PackageManager): List<InstalledAppInfo> {
    return withContext(Dispatchers.Default) {
        try {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            apps.filter { appInfo ->
                // Only show apps that have a launcher activity (user-facing apps)
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }.map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appName = try {
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        appInfo.packageName
                    },
                    icon = try {
                        packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (_: Exception) {
                        null
                    }
                )
            }.sortedBy { it.appName.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureAppsScreen(
    defaultApps: Set<String>,
    configuredApps: Set<String>,
    excludedApps: Set<String>,
    isPremiumUser: Boolean,
    onBack: () -> Unit,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onAddExcludedApp: (String) -> Unit,
    onRemoveExcludedApp: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var appToRemove by remember { mutableStateOf<String?>(null) }
    var showAddExcludedDialog by remember { mutableStateOf(false) }
    var appToRemoveFromExcluded by remember { mutableStateOf<String?>(null) }

    // Handle back press
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Financial Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section: Default Apps (Cannot be removed)
            item {
                Text(
                    text = "Default Financial Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "These apps are always monitored and cannot be removed. Any apps containing the words \"bank\", \"pay\", \"wallet\", \"finance\", or \"money\" may also be monitored automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(defaultApps.sorted()) { packageName ->
                AppListItem(
                    packageName = packageName,
                    isRemovable = false,
                    onRemove = {}
                )
            }

            // Section: Custom Apps (Can be removed)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Custom Financial Apps (${configuredApps.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = if (configuredApps.isNotEmpty())
                                "Apps you've added - tap × to remove"
                            else
                                "No custom apps yet - tap + to add",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (!isPremiumUser) {
                            Text(
                                text = "Free tier: ${configuredApps.size}/3 apps used",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (configuredApps.size >= 3)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (isPremiumUser || configuredApps.size < 3) {
                                showAddDialog = true
                            } else {
                                // For free users at limit, navigate back first then show upgrade dialog
                                onBack()
                                // Trigger a dummy app to show upgrade dialog
                                onAddApp("")
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add App",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (configuredApps.isNotEmpty()) {
                items(configuredApps.sorted()) { packageName ->
                    AppListItem(
                        packageName = packageName,
                        isRemovable = true,
                        onRemove = { appToRemove = packageName }
                    )
                }
            }

            // Section: Excluded Apps
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Excluded Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = if (excludedApps.isNotEmpty())
                                "Apps that won't be monitored - tap × to remove"
                            else
                                "Exclude apps from monitoring - tap + to add",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { showAddExcludedDialog = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Excluded App",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (excludedApps.isNotEmpty()) {
                items(excludedApps.sorted()) { packageName ->
                    AppListItem(
                        packageName = packageName,
                        isRemovable = true,
                        isExcluded = true,
                        onRemove = { appToRemoveFromExcluded = packageName }
                    )
                }
            }
        }
    }

    // Add App Dialog
    if (showAddDialog) {
        AppSelectorDialog(
            title = "Add Financial App",
            description = "Select an app to monitor for financial notifications:",
            onDismiss = { showAddDialog = false },
            onConfirm = { packageName ->
                onAddApp(packageName)
                showAddDialog = false
            }
        )
    }

    // Add Excluded App Dialog
    if (showAddExcludedDialog) {
        AppSelectorDialog(
            title = "Exclude App",
            description = "Select an app to exclude from monitoring:",
            onDismiss = { showAddExcludedDialog = false },
            onConfirm = { packageName ->
                onAddExcludedApp(packageName)
                showAddExcludedDialog = false
            }
        )
    }

    // Remove Confirmation Dialog
    if (appToRemove != null) {
        AlertDialog(
            onDismissRequest = { appToRemove = null },
            title = { Text("Remove App") },
            text = {
                Text("Are you sure you want to stop monitoring notifications from:\n\n$appToRemove")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appToRemove?.let { onRemoveApp(it) }
                        appToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { appToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Excluded App Confirmation Dialog
    if (appToRemoveFromExcluded != null) {
        AlertDialog(
            onDismissRequest = { appToRemoveFromExcluded = null },
            title = { Text("Remove from Exclusion List") },
            text = {
                Text("Remove $appToRemoveFromExcluded from the exclusion list?\n\nIt may be monitored again if it matches financial app patterns.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appToRemoveFromExcluded?.let { onRemoveExcludedApp(it) }
                        appToRemoveFromExcluded = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { appToRemoveFromExcluded = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppListItem(
    packageName: String,
    isRemovable: Boolean,
    isExcluded: Boolean = false,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isExcluded -> MaterialTheme.colorScheme.errorContainer
                isRemovable -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getAppDisplayName(packageName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isRemovable) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AppSelectorDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Load installed apps
    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(packageManager)
        isLoading = false
    }

    // Filter apps based on search query
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps") },
                    placeholder = { Text("App name or package") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // App list
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank())
                                "No apps found"
                            else
                                "No apps match your search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onConfirm(app.packageName)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App icon
                                    if (app.icon != null) {
                                        val bitmap = remember(app.icon) {
                                            app.icon.toBitmap().asImageBitmap()
                                        }
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "${app.appName} icon",
                                            modifier = Modifier.size(40.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Default icon",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // App info
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddAppDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = packageName,
                    onValueChange = {
                        packageName = it
                        errorMessage = null
                    },
                    label = { Text("Package Name") },
                    placeholder = { Text("com.example.bank") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tip: You can find an app's package name by searching for it online or using package name finder apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (packageName.isBlank()) {
                        errorMessage = "Package name cannot be empty"
                    } else if (!packageName.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))) {
                        errorMessage = "Invalid package name format"
                    } else {
                        onConfirm(packageName)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to get friendly app names
fun getAppDisplayName(packageName: String): String {
    return when (packageName) {
        "com.chase.sig.android" -> "Chase"
        "com.wf.wellsfargomobile" -> "Wells Fargo"
        "com.bankofamerica.cashpromobile" -> "Bank of America"
        "com.citi.citimobile" -> "Citi"
        "com.usaa.mobile.android.usaa" -> "USAA"
        "com.konylabs.capitalone" -> "Capital One"
        "com.discover.mobile" -> "Discover"
        "com.americanexpress.android.acctsvcs.us" -> "American Express"
        "com.pnc.ecommerce.mobile" -> "PNC Bank"
        "com.venmo" -> "Venmo"
        "com.paypal.android.p2pmobile" -> "PayPal"
        "com.squareup.cash" -> "Cash App"
        "com.google.android.apps.walletnfcrel" -> "Google Pay"
        "com.apple.android.music" -> "Apple Pay"
        "com.zellepay.zelle" -> "Zelle"
        "com.mand.notitest" -> "Test App"
        else -> packageName.split(".").lastOrNull()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            ?: packageName
    }
}


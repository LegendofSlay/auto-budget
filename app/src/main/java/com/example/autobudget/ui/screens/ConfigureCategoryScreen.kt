package com.example.autobudget.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.autobudget.data.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureCategoryScreen(
    defaultMappings: Map<String, String>,
    customMappings: Map<String, String>,
    categories: Set<String>,
    onBack: () -> Unit,
    onAddMapping: (String, String) -> Unit,
    onRemoveMapping: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var mappingToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToRemove by remember { mutableStateOf<String?>(null) }

    // Handle back press
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Category Mapping") },
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
            // Section: Manage Categories
            item {
                Text(
                    text = "Manage Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Add or remove categories to use in your mappings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Section: Categories
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Categories (${categories.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "Available categories for transaction mapping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Category",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(categories.sorted()) { category ->
                CategoryCard(
                    category = category,
                    isDefault = PreferencesManager.DEFAULT_CATEGORIES.contains(category),
                    onRemove = { categoryToRemove = category }
                )
            }

            // Divider between sections
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Category Mappings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Map keywords to categories for automatic categorization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Section: Custom Mappings (Can be removed)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Custom Category Mappings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = if (customMappings.isNotEmpty())
                                "Your custom mappings - tap × to remove"
                            else
                                "No custom mappings yet - tap + to add",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Mapping",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (customMappings.isNotEmpty()) {
                items(customMappings.toList().sortedBy { it.first }) { (keyword, category) ->
                    CategoryMappingCard(
                        keyword = keyword,
                        category = category,
                        isRemovable = true,
                        onRemove = { mappingToRemove = keyword to category }
                    )
                }
            }
        }
    }

    // Add Mapping Dialog
    if (showAddDialog) {
        AddCategoryMappingDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { keyword, category ->
                onAddMapping(keyword, category)
                showAddDialog = false
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { categoryName ->
                onAddCategory(categoryName)
                showAddCategoryDialog = false
            }
        )
    }

    // Remove Category Confirmation Dialog
    if (categoryToRemove != null) {
        AlertDialog(
            onDismissRequest = { categoryToRemove = null },
            title = { Text("Remove Category") },
            text = {
                Text("Are you sure you want to remove the category:\n\n\"$categoryToRemove\"\n\nNote: This will not remove any mappings using this category.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToRemove?.let { onRemoveCategory(it) }
                        categoryToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Confirmation Dialog
    if (mappingToRemove != null) {
        AlertDialog(
            onDismissRequest = { mappingToRemove = null },
            title = { Text("Remove Category Mapping") },
            text = {
                Text("Are you sure you want to remove the mapping:\n\n\"${mappingToRemove?.first}\" → ${mappingToRemove?.second}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mappingToRemove?.let { onRemoveMapping(it.first) }
                        mappingToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { mappingToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryMappingCard(
    keyword: String,
    category: String,
    isRemovable: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRemovable)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "\"$keyword\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Transactions containing \"$keyword\" will be categorized as $category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryMappingDialog(
    categories: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category Mapping") },
        text = {
            Column {
                Text(
                    text = "Create a custom category mapping. Transactions containing the keyword will be automatically categorized.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = keyword,
                    onValueChange = {
                        keyword = it
                        errorMessage = null
                    },
                    label = { Text("Keyword") },
                    placeholder = { Text("e.g., starbucks") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        Text(
                            text = "Enter a keyword to match in transaction descriptions",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        placeholder = { Text("Select a category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        isError = errorMessage != null,
                        supportingText = {
                            Text(
                                text = "Select a category from the list",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        categories.sorted().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        keyword.isBlank() -> errorMessage = "Keyword cannot be empty"
                        selectedCategory.isBlank() -> errorMessage = "Category must be selected"
                        else -> onConfirm(keyword.trim(), selectedCategory)
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

@Composable
fun CategoryCard(
    category: String,
    isDefault: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.secondaryContainer
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (isDefault) "Built-in category" else "Custom category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!isDefault) {
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
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Category") },
        text = {
            Column {
                Text(
                    text = "Create a new category that can be used when mapping keywords to transactions.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        errorMessage = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g., Entertainment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        Text(
                            text = "Enter a descriptive name for the category",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        categoryName.isBlank() -> errorMessage = "Category name cannot be empty"
                        else -> onConfirm(categoryName.trim())
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

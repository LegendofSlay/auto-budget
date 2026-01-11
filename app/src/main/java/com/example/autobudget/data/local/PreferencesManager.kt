package com.example.autobudget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val SPREADSHEET_NAME = stringPreferencesKey("spreadsheet_name")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val FINANCIAL_APPS = stringSetPreferencesKey("financial_apps")
        val EXCLUDED_APPS = stringSetPreferencesKey("excluded_apps")
        val CATEGORY_MAPPINGS = stringPreferencesKey("category_mappings")
        val CATEGORIES = stringSetPreferencesKey("categories")

        // Default categories
        val DEFAULT_CATEGORIES = setOf(
            "Coffee/Snacks",
            "Food",
            "Health/Medical",
            "Rent/Utilities",
            "Home",
            "Personal",
            "Transportation",
            "Dining/Fast Food",
            "Travel"
        )
    }

    val spreadsheetId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SPREADSHEET_ID]
    }

    val spreadsheetName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SPREADSHEET_NAME]
    }

    val googleAccountEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOOGLE_ACCOUNT_EMAIL]
    }

    val financialApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FINANCIAL_APPS] ?: emptySet()
    }

    val excludedApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[EXCLUDED_APPS] ?: emptySet()
    }

    val categoryMappings: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[CATEGORY_MAPPINGS] ?: ""
        if (jsonString.isBlank()) {
            emptyMap()
        } else {
            // Parse JSON string to map
            parseCategoryMappings(jsonString)
        }
    }

    val categories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val customCategories = preferences[CATEGORIES] ?: emptySet()
        DEFAULT_CATEGORIES + customCategories
    }

    private fun parseCategoryMappings(jsonString: String): Map<String, String> {
        return try {
            // Simple parsing: "keyword1:category1,keyword2:category2"
            jsonString.split(",").mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else null
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeCategoryMappings(mappings: Map<String, String>): String {
        return mappings.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    suspend fun saveSpreadsheetConfig(spreadsheetId: String, spreadsheetName: String) {
        context.dataStore.edit { preferences ->
            preferences[SPREADSHEET_ID] = spreadsheetId
            preferences[SPREADSHEET_NAME] = spreadsheetName
        }
    }

    suspend fun saveFinancialApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FINANCIAL_APPS] = apps
        }
    }

    suspend fun addFinancialApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentApps = preferences[FINANCIAL_APPS] ?: emptySet()
            preferences[FINANCIAL_APPS] = currentApps + packageName
        }
    }

    suspend fun removeFinancialApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentApps = preferences[FINANCIAL_APPS] ?: emptySet()
            preferences[FINANCIAL_APPS] = currentApps - packageName
        }
    }

    suspend fun addExcludedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentExcluded = preferences[EXCLUDED_APPS] ?: emptySet()
            preferences[EXCLUDED_APPS] = currentExcluded + packageName
        }
    }

    suspend fun removeExcludedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentExcluded = preferences[EXCLUDED_APPS] ?: emptySet()
            preferences[EXCLUDED_APPS] = currentExcluded - packageName
        }
    }

    suspend fun saveCategoryMappings(mappings: Map<String, String>) {
        context.dataStore.edit { preferences ->
            preferences[CATEGORY_MAPPINGS] = serializeCategoryMappings(mappings)
        }
    }

    suspend fun addCategoryMapping(keyword: String, category: String) {
        context.dataStore.edit { preferences ->
            val currentMappings = parseCategoryMappings(preferences[CATEGORY_MAPPINGS] ?: "")
            val updatedMappings = currentMappings + (keyword.lowercase() to category)
            preferences[CATEGORY_MAPPINGS] = serializeCategoryMappings(updatedMappings)
        }
    }

    suspend fun removeCategoryMapping(keyword: String) {
        context.dataStore.edit { preferences ->
            val currentMappings = parseCategoryMappings(preferences[CATEGORY_MAPPINGS] ?: "")
            val updatedMappings = currentMappings - keyword.lowercase()
            preferences[CATEGORY_MAPPINGS] = serializeCategoryMappings(updatedMappings)
        }
    }

    suspend fun addCategory(category: String) {
        context.dataStore.edit { preferences ->
            val currentCategories = preferences[CATEGORIES] ?: emptySet()
            preferences[CATEGORIES] = currentCategories + category
        }
    }

    suspend fun removeCategory(category: String) {
        context.dataStore.edit { preferences ->
            val currentCategories = preferences[CATEGORIES] ?: emptySet()
            preferences[CATEGORIES] = currentCategories - category
        }
    }

    suspend fun saveGoogleAccount(email: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_ACCOUNT_EMAIL] = email
        }
    }

    suspend fun clearGoogleAccount() {
        context.dataStore.edit { preferences ->
            preferences.remove(GOOGLE_ACCOUNT_EMAIL)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}


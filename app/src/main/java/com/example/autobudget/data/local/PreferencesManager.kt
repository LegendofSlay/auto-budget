package com.example.autobudget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val SPREADSHEET_NAME = stringPreferencesKey("spreadsheet_name")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
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

    suspend fun saveSpreadsheetConfig(spreadsheetId: String, spreadsheetName: String) {
        context.dataStore.edit { preferences ->
            preferences[SPREADSHEET_ID] = spreadsheetId
            preferences[SPREADSHEET_NAME] = spreadsheetName
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


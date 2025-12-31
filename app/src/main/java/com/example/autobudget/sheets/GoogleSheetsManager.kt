package com.example.autobudget.sheets

import android.content.Context
import com.example.autobudget.data.model.Transaction
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleSheetsManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

    private fun getSheetsService(): Sheets? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("AutoBudget")
            .build()
    }

    /**
     * Gets the spreadsheet metadata
     */
    suspend fun getSpreadsheet(spreadsheetId: String): Result<Spreadsheet> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService() ?: return@withContext Result.failure(
                Exception("Not signed in to Google")
            )

            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            Result.success(spreadsheet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Appends a transaction as a new row in the "Transactions" sheet
     */
    suspend fun appendTransaction(
        spreadsheetId: String,
        transaction: Transaction
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService() ?: return@withContext Result.failure(
                Exception("Not signed in to Google")
            )

            // Prepare the row data
            val row = listOf<Any>(
                dateFormat.format(Date(transaction.timestamp)),
                transaction.amount.toString(),
                transaction.merchantName,
                "Auto Budget"
            )

            val body = ValueRange().setValues(listOf(row))

            // Append to the "Transactions" sheet
            service.spreadsheets().values()
                .append(spreadsheetId, "Transactions!B:E", body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("OVERWRITE")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates the header row in the Transactions sheet if it doesn't exist
     */
    suspend fun ensureHeadersExist(spreadsheetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService() ?: return@withContext Result.failure(
                Exception("Not signed in to Google")
            )

            // Check if there's already data
            val response = service.spreadsheets().values()
                .get(spreadsheetId, "Transactions!A1:F1")
                .execute()

            val values = response.getValues()
            if (values.isNullOrEmpty()) {
                // Add headers
                val headers = listOf<Any>(
                    "Date",
                    "Amount",
                    "Description",
                    "Category"
                )

                val body = ValueRange().setValues(listOf(headers))

                service.spreadsheets().values()
                    .update(spreadsheetId, "Transactions!B4:E4", body)
                    .setValueInputOption("RAW")
                    .execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Sheet might not exist, try to continue anyway
            Result.failure(e)
        }
    }

    /**
     * Validates that the spreadsheet ID is accessible
     */
    suspend fun validateSpreadsheet(spreadsheetId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService() ?: return@withContext Result.failure(
                Exception("Not signed in to Google")
            )

            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val title = spreadsheet.properties.title

            Result.success(title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts spreadsheet ID from a Google Sheets URL
     */
    fun extractSpreadsheetId(urlOrId: String): String? {
        // If it's already just an ID
        if (!urlOrId.contains("/")) {
            return urlOrId.trim()
        }

        // Extract from URL: https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit
        val regex = """/spreadsheets/d/([a-zA-Z0-9-_]+)""".toRegex()
        val match = regex.find(urlOrId)
        return match?.groupValues?.get(1)
    }
}


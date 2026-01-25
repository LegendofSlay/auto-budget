package com.techstackers.autobudget.sheets

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    private val _signInState = MutableStateFlow<GoogleSignInState>(GoogleSignInState.NotSignedIn)
    val signInState: StateFlow<GoogleSignInState> = _signInState.asStateFlow()

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(SheetsScopes.SPREADSHEETS),
                Scope(SheetsScopes.DRIVE_FILE)
            )
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    init {
        // Check for existing sign-in
        checkExistingSignIn()
    }

    private fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && hasRequiredScopes(account)) {
            _currentAccount.value = account
            _signInState.value = GoogleSignInState.SignedIn(account.email ?: "Unknown")
            Log.d(TAG, "Existing sign-in found: ${account.email}")
        } else {
            _signInState.value = GoogleSignInState.NotSignedIn
            Log.d(TAG, "No existing sign-in found")
        }
    }

    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        val sheetsScope = Scope(SheetsScopes.SPREADSHEETS)
        val driveScope = Scope(SheetsScopes.DRIVE_FILE)
        return GoogleSignIn.hasPermissions(account, sheetsScope, driveScope)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()
            _currentAccount.value = account
            _signInState.value = GoogleSignInState.SignedIn(account.email ?: "Unknown")
            Log.d(TAG, "Sign-in successful: ${account.email}")
            Result.success(account)
        } catch (e: ApiException) {
            val errorMessage = getSignInErrorMessage(e.statusCode)
            Log.e(TAG, "Sign-in failed with status code: ${e.statusCode} - $errorMessage", e)
            _signInState.value = GoogleSignInState.Error(errorMessage)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed with unexpected error", e)
            _signInState.value = GoogleSignInState.Error(e.message ?: "Sign-in failed")
            Result.failure(e)
        }
    }

    private fun getSignInErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in was cancelled"
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in already in progress"
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign-in failed"
            GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                "DEVELOPER_ERROR (code 10): Check that SHA-1 fingerprint and package name " +
                "(com.techstackers.autobudget) are correctly configured in Google Cloud Console"
            GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error - check internet connection"
            GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account"
            GoogleSignInStatusCodes.INTERNAL_ERROR -> "Internal error"
            12501 -> "User cancelled the sign-in flow"
            12502 -> "Sign-in attempt didn't succeed with the current account"
            else -> "Unknown error (code: $statusCode)"
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            _currentAccount.value = null
            _signInState.value = GoogleSignInState.NotSignedIn
            Log.d(TAG, "Sign-out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
            _signInState.value = GoogleSignInState.Error(e.message ?: "Sign-out failed")
        }
    }

    fun getAccount(): GoogleSignInAccount? {
        return _currentAccount.value ?: GoogleSignIn.getLastSignedInAccount(context)
    }
}

sealed class GoogleSignInState {
    object NotSignedIn : GoogleSignInState()
    data class SignedIn(val email: String) : GoogleSignInState()
    data class Error(val message: String) : GoogleSignInState()
    object Loading : GoogleSignInState()
}


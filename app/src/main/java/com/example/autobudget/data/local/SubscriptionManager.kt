package com.example.autobudget.data.local

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.autobudget.billing.BillingManager
import com.example.autobudget.billing.PurchaseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Manages subscription/premium status for the app.
 *
 * Free tier limitations:
 * - Cannot add custom categories
 * - Maximum 10 category mappings
 * - Maximum 3 financial apps
 *
 * Premium tier: Unlimited access to all features
 */
class SubscriptionManager(
    private val context: Context,
    private val billingManager: BillingManager,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "SubscriptionManager"
        private val IS_PREMIUM_USER = booleanPreferencesKey("is_premium_user")
        private val DEBUG_PREMIUM_OVERRIDE = booleanPreferencesKey("debug_premium_override")

        // Free tier limits
        const val FREE_CATEGORY_MAPPINGS_LIMIT = 10
        const val FREE_FINANCIAL_APPS_LIMIT = 3
    }

    /**
     * Flow that emits the current premium status
     * Combines Google Play Billing status with debug override
     */
    val isPremiumUser: Flow<Boolean> = combine(
        context.dataStore.data.map { preferences ->
            preferences[IS_PREMIUM_USER] ?: false
        },
        context.dataStore.data.map { preferences ->
            preferences[DEBUG_PREMIUM_OVERRIDE] ?: false
        }
    ) { storedPremium, debugOverride ->
        // Debug override takes precedence for testing
        if (debugOverride) {
            Log.d(TAG, "Debug premium override active")
            return@combine true
        }
        storedPremium
    }

    init {
        // Listen for purchase results and update premium status
        scope.launch {
            billingManager.purchaseResult.collect { result ->
                when (result) {
                    is PurchaseResult.Success, is PurchaseResult.AlreadyOwned -> {
                        // Verify the purchase and update premium status
                        syncPremiumStatusWithBilling()
                    }
                    else -> {
                        // Purchase failed or was cancelled, no action needed
                    }
                }
            }
        }

        // Sync premium status on initialization
        scope.launch {
            syncPremiumStatusWithBilling()
        }
    }

    /**
     * Sync premium status with Google Play Billing
     */
    private suspend fun syncPremiumStatusWithBilling() {
        val hasPremium = billingManager.hasPremiumSubscription()
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM_USER] = hasPremium
        }
        Log.d(TAG, "Premium status synced: $hasPremium")
    }

    /**
     * Launch the purchase flow for premium lifetime
     *
     * @param activity The activity to launch the billing flow from
     */
    fun upgradeToPremium(activity: Activity) {
        Log.d(TAG, "Launching purchase flow for: ${BillingManager.PRODUCT_PREMIUM_LIFETIME}")
        billingManager.launchPurchaseFlow(activity, BillingManager.PRODUCT_PREMIUM_LIFETIME)
    }

    /**
     * Manually refresh premium status from Google Play Billing
     */
    suspend fun refreshPremiumStatus() {
        billingManager.queryPurchases()
        syncPremiumStatusWithBilling()
    }

    /**
     * DEBUG ONLY: Toggle premium status for testing without actual purchase
     * This uses a separate debug flag that doesn't interfere with real billing
     */
    suspend fun debugTogglePremium() {
        context.dataStore.edit { preferences ->
            val current = preferences[DEBUG_PREMIUM_OVERRIDE] ?: false
            preferences[DEBUG_PREMIUM_OVERRIDE] = !current
            Log.d(TAG, "Debug premium override toggled to: ${!current}")
        }
    }

    /**
     * DEBUG ONLY: Clear debug premium override
     */
    suspend fun debugClearPremiumOverride() {
        context.dataStore.edit { preferences ->
            preferences[DEBUG_PREMIUM_OVERRIDE] = false
        }
    }

    /**
     * Checks if user can add a category (Premium only)
     */
    fun canAddCategory(isPremium: Boolean): Boolean {
        return isPremium
    }

    /**
     * Checks if user can add a category mapping
     */
    fun canAddCategoryMapping(isPremium: Boolean, currentCount: Int): Boolean {
        if (isPremium) return true
        return currentCount < FREE_CATEGORY_MAPPINGS_LIMIT
    }

    /**
     * Checks if user can add a financial app
     */
    fun canAddFinancialApp(isPremium: Boolean, currentCount: Int): Boolean {
        if (isPremium) return true
        return currentCount < FREE_FINANCIAL_APPS_LIMIT
    }
}

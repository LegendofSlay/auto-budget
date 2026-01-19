package com.example.autobudget.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages Google Play Billing for in-app purchases and subscriptions.
 *
 * Product IDs (configure these in Google Play Console):
 * - autobudget_premium_lifetime: Lifetime subscription
 */
class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // Product ID - this must match the ID created in Google Play Console
        const val PRODUCT_PREMIUM_LIFETIME = "autobudget_premium_lifetime"
    }

    private var billingClient: BillingClient? = null

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()

    private val _purchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val purchaseResult: StateFlow<PurchaseResult?> = _purchaseResult.asStateFlow()

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToBillingService()
    }

    private fun connectToBillingService() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    _connectionState.value = BillingConnectionState.CONNECTED

                    // Query available products
                    queryProducts()

                    // Query existing purchases
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client connection failed: ${billingResult.debugMessage}")
                    _connectionState.value = BillingConnectionState.ERROR
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing client disconnected")
                _connectionState.value = BillingConnectionState.DISCONNECTED
                // Try to reconnect
                connectToBillingService()
            }
        })
    }

    /**
     * Query available product from Google Play
     */
    private fun queryProducts() {
        scope.launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_PREMIUM_LIFETIME)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            withContext(Dispatchers.IO) {
                billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Products queried successfully: ${productDetailsList.size} products")
                        _availableProducts.value = productDetailsList
                    } else {
                        Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    /**
     * Query existing purchases to check if user already has premium
     */
    fun queryPurchases() {
        scope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            withContext(Dispatchers.IO) {
                billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchases queried: ${purchases.size} active purchases")
                        handlePurchases(purchases)
                    } else {
                        Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    /**
     * Launch the purchase flow for lifetime premium
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productDetails = _availableProducts.value.find { it.productId == productId }

        if (productDetails == null) {
            Log.e(TAG, "Product not found: $productId")
            _purchaseResult.value = PurchaseResult.Error("Product not available")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Handle purchase updates from Google Play
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    Log.d(TAG, "Purchase successful: ${purchases.size} purchases")
                    handlePurchases(purchases)
                    _purchaseResult.value = PurchaseResult.Success
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase canceled by user")
                _purchaseResult.value = PurchaseResult.Cancelled
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                _purchaseResult.value = PurchaseResult.AlreadyOwned
                queryPurchases() // Refresh purchases
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _purchaseResult.value = PurchaseResult.Error(billingResult.debugMessage)
            }
        }
    }

    /**
     * Process and acknowledge purchases
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        scope.launch {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Verify purchase (in production, verify on your backend server)
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    /**
     * Acknowledge a purchase
     */
    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        withContext(Dispatchers.IO) {
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged")
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }
    }

    /**
     * Check if user has purchased lifetime premium
     */
    suspend fun hasPremiumSubscription(): Boolean {
        return withContext(Dispatchers.IO) {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            var hasPremium = false

            billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    hasPremium = purchases.any { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(PRODUCT_PREMIUM_LIFETIME)
                    }
                }
            }

            hasPremium
        }
    }

    /**
     * Clear the purchase result after it has been handled
     */
    fun clearPurchaseResult() {
        _purchaseResult.value = null
    }

    /**
     * End the billing client connection
     */
    fun endConnection() {
        billingClient?.endConnection()
        _connectionState.value = BillingConnectionState.DISCONNECTED
    }
}

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

sealed class PurchaseResult {
    object Success : PurchaseResult()
    object Cancelled : PurchaseResult()
    object AlreadyOwned : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

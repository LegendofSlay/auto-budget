# Google Play Billing Implementation Guide

## Overview
This document describes the complete Google Play Billing integration for AutoBudget, which enables users to purchase lifetime premium access.

## Product Configuration

### Single Product ID
**Product ID**: `autobudget_premium_lifetime`
**Type**: In-App Purchase (One-time purchase)
**Description**: Lifetime premium access to all features

### Google Play Console Setup Required

1. **Create In-App Product**:
   - Go to Google Play Console → Your App → Monetization → In-app products
   - Click "Create product"
   - Product ID: `autobudget_premium_lifetime`
   - Name: "Premium Lifetime Access"
   - Description: "Unlock all premium features with a one-time purchase"
   - Default price: Set your desired price (e.g., $9.99)
   - Status: Active

2. **Configure Pricing**:
   - Set prices for different countries/regions
   - Google will convert based on local currency

3. **Publish**:
   - Save and activate the product
   - Product must be active before it can be purchased

## Architecture

### BillingManager (`billing/BillingManager.kt`)
Core Google Play Billing client wrapper that handles:
- Billing client initialization and connection
- Product query (fetches product details from Google Play)
- Purchase flow launching
- Purchase verification and acknowledgment
- Purchase state management

**Key Features**:
- Automatic reconnection on disconnect
- Purchase result flow for observing purchase events
- Product availability checking
- Automatic purchase acknowledgment

### SubscriptionManager (`data/local/SubscriptionManager.kt`)
Higher-level subscription logic that manages:
- Premium status synchronization with billing
- Local premium status storage (DataStore)
- Debug override for testing without real purchases
- Integration with app features

**Premium Status Logic**:
```kotlin
isPremiumUser = debugOverride OR billingPurchaseStatus
```

### HomeViewModel Integration
- `upgradeToPremium(context)`: Launches billing flow
- `debugTogglePremium()`: Debug-only toggle for testing
- Passes Activity context to billing for purchase flow

## Implementation Files

### New Files Created
1. **BillingManager.kt**: `/app/src/main/java/com/example/autobudget/billing/BillingManager.kt`
   - 280+ lines of billing logic
   - Handles all Google Play Billing interactions

### Modified Files
1. **SubscriptionManager.kt**: Now integrates with BillingManager
2. **AutoBudgetApplication.kt**: Creates BillingManager instance
3. **HomeViewModel.kt**: Uses real billing flow, renamed debug function
4. **HomeScreen.kt**: Passes context to billing flow, updated debug menu
5. **MainActivity.kt**: No changes needed
6. **AndroidManifest.xml**: Added BILLING permission
7. **build.gradle.kts**: Added billing dependency

## How It Works

### Purchase Flow

1. **User clicks "Upgrade Now" in UpgradeDialog**
   ```
   User Action → HomeScreen → HomeViewModel.upgradeToPremium(context)
   ```

2. **ViewModel dismisses dialog and launches billing**
   ```
   upgradeToPremium() → SubscriptionManager.upgradeToPremium(activity)
   ```

3. **SubscriptionManager calls BillingManager**
   ```
   upgradeToPremium() → BillingManager.launchPurchaseFlow(activity, productId)
   ```

4. **BillingManager shows Google Play purchase UI**
   ```
   launchBillingFlow() → Google Play handles payment
   ```

5. **Purchase result received**
   ```
   onPurchasesUpdated() → handlePurchases() → acknowledgePurchase()
   ```

6. **Premium status synced**
   ```
   PurchaseResult.Success → syncPremiumStatusWithBilling() → Update DataStore
   ```

### Status Synchronization

**On App Launch**:
```
AutoBudgetApplication.onCreate()
  → BillingManager.init()
  → SubscriptionManager.init()
  → syncPremiumStatusWithBilling()
  → Query purchases from Google Play
  → Update local premium status
```

**On Purchase Complete**:
```
onPurchasesUpdated(SUCCESS)
  → handlePurchases()
  → acknowledgePurchase()
  → syncPremiumStatusWithBilling()
  → isPremiumUser emits true
  → UI updates automatically
```

### Purchase States

| State | Description | Action |
|-------|-------------|--------|
| `SUCCESS` | Purchase completed | Acknowledge & sync status |
| `CANCELLED` | User cancelled | Show cancellation message |
| `ALREADY_OWNED` | Already purchased | Refresh purchases & sync |
| `ERROR` | Purchase failed | Show error message |

## Testing

### Debug Toggle (Development Only)

A debug toggle is available for testing without real purchases:

**Location**: Settings Menu → "Toggle Premium (Debug Testing)"

**Function**: `HomeViewModel.debugTogglePremium()`
- Toggles a separate `DEBUG_PREMIUM_OVERRIDE` flag
- Does NOT interfere with real billing status
- Persists across app restarts
- Takes precedence over billing status

**Use Cases**:
- Test premium features during development
- Demo premium features
- QA testing without actual purchases

### Testing Real Billing

1. **Test with Test Account**:
   - Add test Gmail accounts in Google Play Console
   - Test accounts can make purchases without being charged
   - Purchases are real but free for test accounts

2. **Testing Purchase Flow**:
   ```
   1. Remove debug override (if active)
   2. Ensure signed APK/AAB
   3. Upload to Internal Testing track
   4. Download from Play Store (internal test)
   5. Attempt purchase with test account
   ```

3. **Testing Purchase Restoration**:
   ```
   1. Make a test purchase
   2. Uninstall app
   3. Reinstall app
   4. Premium status should restore automatically
   ```

## Security Considerations

### Current Implementation
- Purchases are verified client-side
- Purchase acknowledgment prevents refund abuse
- Product ID validated before purchase

### Production Recommendations

⚠️ **IMPORTANT**: Add server-side verification before production!

**Why Server-Side Verification?**:
- Prevent purchase spoofing
- Validate purchase tokens with Google's API
- Detect fraudulent purchases
- Protect against modified APKs

**Implementation Steps**:
1. Create backend API endpoint
2. Send purchase token to your server
3. Server verifies with Google Play Developer API
4. Server returns verification result
5. Only then update premium status

**Google Play Developer API**:
```
POST https://androidpublisher.googleapis.com/androidpublisher/v3/
  applications/{packageName}/purchases/products/{productId}/tokens/{token}
```

### Additional Security

1. **Obfuscation**: Enable ProGuard in release builds
2. **Certificate Pinning**: Pin to Google Play services
3. **Root Detection**: Consider detecting rooted devices
4. **Tamper Detection**: Validate APK signature

## Dependency

Added to `app/build.gradle.kts`:
```kotlin
implementation("com.android.billingclient:billing-ktx:7.1.1")
```

**Version**: 7.1.1 (Latest as of January 2026)
- Kotlin extensions for cleaner code
- Coroutine support built-in
- Latest billing library features

## Permissions

Added to `AndroidManifest.xml`:
```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

## Error Handling

### Connection Errors
```kotlin
BillingConnectionState.ERROR
  → Automatic reconnection attempt
  → Log error message
  → User sees "Unable to connect to billing service"
```

### Purchase Errors
```kotlin
PurchaseResult.Error(message)
  → Show error to user
  → Log detailed error
  → User can retry purchase
```

### Product Not Found
```kotlin
Product query returns empty list
  → Log warning
  → Disable upgrade button
  → Show "Premium not available" message
```

## State Management

### Flows
```kotlin
// BillingManager
connectionState: StateFlow<BillingConnectionState>
availableProducts: StateFlow<List<ProductDetails>>
purchaseResult: StateFlow<PurchaseResult?>

// SubscriptionManager
isPremiumUser: Flow<Boolean>  // Combined billing + debug status

// HomeViewModel
isPremiumUser: StateFlow<Boolean>  // For UI observation
```

### DataStore Keys
```kotlin
IS_PREMIUM_USER: Boolean           // Synced from billing
DEBUG_PREMIUM_OVERRIDE: Boolean    // Debug only
```

## Troubleshooting

### "Product not available"
- Check product is active in Google Play Console
- Verify product ID matches exactly
- Ensure app is signed and uploaded to Play Console
- Check app version code matches uploaded version

### "Unable to launch purchase flow"
- Context must be an Activity (not Application context)
- Check BILLING permission in manifest
- Ensure billing dependency is added
- Verify Google Play Services is installed

### Purchase not restoring
- Check `queryPurchases()` is called on app launch
- Verify purchase was acknowledged
- Check logs for billing errors
- Ensure same Google account is signed in

### Debug toggle not working
- Check logs for "Debug premium override" message
- Verify DataStore is persisting data
- Clear app data if needed

## Production Checklist

Before releasing to production:

- [ ] Create product in Google Play Console
- [ ] Set pricing for all regions
- [ ] Activate the product
- [ ] Upload signed release build
- [ ] Test with real Google account (internal testing)
- [ ] Test purchase flow end-to-end
- [ ] Test purchase restoration after reinstall
- [ ] Implement server-side verification (recommended)
- [ ] Enable ProGuard obfuscation
- [ ] Test on multiple Android versions
- [ ] Remove/hide debug toggle in release builds
- [ ] Add analytics for purchase events
- [ ] Set up Play Console webhooks for notifications
- [ ] Document refund policy

## Support & Refunds

### User Refund Requests
Google Play handles refunds automatically:
- Users can request refunds through Play Store
- Refunds within 48 hours are automatic
- After 48 hours, developer reviews request
- When refunded, purchase becomes invalid
- App will sync and remove premium status

### Handling Refunds in App
```kotlin
// Automatic via purchase query
queryPurchases() 
  → Returns empty if refunded
  → isPremiumUser becomes false
  → User sees free tier limits again
```

## Analytics Integration

Recommended events to track:

```kotlin
// Purchase Started
logEvent("purchase_started", params)

// Purchase Completed
logEvent("purchase_completed", params {
    "product_id" to PRODUCT_PREMIUM_LIFETIME
    "price" to productDetails.price
    "currency" to productDetails.priceCurrencyCode
})

// Purchase Failed
logEvent("purchase_failed", params {
    "reason" to errorMessage
})

// Purchase Restored
logEvent("purchase_restored", params)
```

## Future Enhancements

Possible additions:

1. **Promotional Pricing**:
   - Add sales/discounts in Play Console
   - Show "limited time offer" in app

2. **Subscription Model**:
   - Change to recurring subscription if desired
   - Add monthly/yearly options
   - Handle subscription states (grace period, on hold, etc.)

3. **Feature Unlock Stages**:
   - Multiple in-app products for different feature sets
   - Tiered pricing (Basic, Pro, Ultra)

4. **Gifting**:
   - Allow users to gift premium to others
   - Generate redemption codes

5. **Family Sharing**:
   - Enable Google Play Family Library sharing

---

**Status**: ✅ Fully Implemented & Ready for Testing
**Product Type**: In-App Purchase (Lifetime)
**Next Steps**: Create product in Google Play Console & test with internal testing track

## Quick Reference

### Product ID
```
autobudget_premium_lifetime
```

### Main Entry Points
```kotlin
// Launch purchase
HomeViewModel.upgradeToPremium(context)

// Check status
HomeViewModel.isPremiumUser.value

// Debug toggle
HomeViewModel.debugTogglePremium()

// Refresh status
SubscriptionManager.refreshPremiumStatus()
```

### Key Files
- `BillingManager.kt` - Core billing logic
- `SubscriptionManager.kt` - Premium status management
- `HomeViewModel.kt` - UI integration
- `AutoBudgetApplication.kt` - Initialization

---

**Implementation Date**: January 19, 2026
**Billing Library**: 7.1.1
**Product Type**: In-App Purchase (One-time)

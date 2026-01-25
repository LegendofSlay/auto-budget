# Premium Features Implementation Guide

## Overview
This document describes the freemium model implementation for AutoBudget, which restricts certain features for free users and prompts them to upgrade to Premium.

## Feature Restrictions

### Free Tier Limitations
1. **Custom Categories**: Cannot add custom categories (Premium only)
2. **Category Mappings**: Limited to 10 custom category mappings
3. **Financial Apps**: Limited to 3 custom financial apps

### Premium Tier
- Unlimited custom categories
- Unlimited category mappings
- Unlimited financial apps
- Priority support

## Implementation Details

### 1. Subscription Manager (`SubscriptionManager.kt`)
- **Location**: `app/src/main/java/com/example/autobudget/data/local/SubscriptionManager.kt`
- **Purpose**: Manages subscription status and validates feature access
- **Key Constants**:
  - `FREE_CATEGORY_MAPPINGS_LIMIT = 10`
  - `FREE_FINANCIAL_APPS_LIMIT = 3`

**Key Methods**:
```kotlin
isPremiumUser: Flow<Boolean>  // Observable premium status
canAddCategory(isPremium: Boolean): Boolean
canAddCategoryMapping(isPremium: Boolean, currentCount: Int): Boolean
canAddFinancialApp(isPremium: Boolean, currentCount: Int): Boolean
upgradeToPremium()  // Mock implementation - replace with IAP
downgradeToFree()   // For testing/support
```

### 2. Upgrade Dialog (`UpgradeDialog.kt`)
- **Location**: `app/src/main/java/com/example/autobudget/ui/components/UpgradeDialog.kt`
- **Purpose**: Reusable dialog component that prompts users to upgrade
- **Features**:
  - Shows feature name being restricted
  - Displays current limit information
  - Lists all premium benefits
  - Material 3 design with star icon

### 3. ViewModel Updates (`HomeViewModel.kt`)
**New State**:
```kotlin
data class HomeUiState(
    val showUpgradeDialog: Boolean = false,
    val upgradeDialogFeature: String = "",
    val upgradeDialogLimit: String? = null
)
```

**New Methods**:
- `dismissUpgradeDialog()`: Close the upgrade dialog
- `upgradeToPremium()`: Handle upgrade action (currently mock)
- `togglePremiumForTesting()`: Development testing toggle

**Modified Methods**:
- `addCategory()`: Checks premium status before allowing
- `addCategoryMapping()`: Validates against 10-mapping limit
- `addFinancialApp()`: Validates against 3-app limit

### 4. UI Updates

#### HomeScreen (`HomeScreen.kt`)
- Collects `isPremiumUser` state from ViewModel
- Passes premium status to child screens
- Shows `UpgradeDialog` when `uiState.showUpgradeDialog` is true
- Added testing menu item to toggle premium status

#### ConfigureAppsScreen (`ConfigureAppsScreen.kt`)
- Accepts `isPremiumUser` parameter
- Shows usage counter for free users: "Free tier: X/3 apps used"
- Counter turns red when limit is reached

#### ConfigureCategoryScreen (`ConfigureCategoryScreen.kt`)
- Accepts `isPremiumUser` parameter
- Shows "⭐ Adding custom categories requires Premium" for free users
- Shows usage counter: "Free tier: X/10 mappings used"
- Counter turns red when limit is reached

### 5. Application Setup (`AutoBudgetApplication.kt`)
Added subscription manager instance:
```kotlin
val subscriptionManager by lazy { SubscriptionManager(this) }
```

## Testing the Implementation

### Toggle Premium Status
1. Open the app
2. Tap the Settings icon (top-right)
3. Select "Toggle Premium (Testing)"
4. Status toggles between Free and Premium

### Test Free Tier Limits

#### Test Category Restriction (Premium Only)
1. Ensure you're on Free tier
2. Go to Settings → Configure Category Mapping
3. Try to add a new category
4. Should see upgrade dialog

#### Test Category Mapping Limit (10 max)
1. Ensure you're on Free tier
2. Go to Settings → Configure Category Mapping
3. Add category mappings until you reach 10
4. Try to add an 11th mapping
5. Should see upgrade dialog

#### Test Financial Apps Limit (3 max)
1. Ensure you're on Free tier
2. Go to Settings → Configure Detected Apps
3. Add custom apps until you reach 3
4. Try to add a 4th app
5. Should see upgrade dialog

### Test Premium Tier
1. Toggle to Premium status
2. All limits should be removed
3. No upgrade dialogs should appear
4. Usage counters should disappear from UI

## Data Storage

Premium status is stored in DataStore:
- **Key**: `is_premium_user` (Boolean)
- **Default**: `false` (Free tier)
- **Location**: Same DataStore as other app preferences

## Future Implementation: Google Play Billing

### TODO: Replace Mock Implementation

The current implementation uses a mock upgrade system for testing. To implement real Google Play Billing:

1. **Add Dependencies** (build.gradle.kts):
```kotlin
implementation("com.android.billingclient:billing-ktx:6.0.1")
```

2. **Create BillingManager**:
   - Initialize BillingClient
   - Query available products
   - Launch purchase flow
   - Handle purchase verification
   - Update SubscriptionManager after successful purchase

3. **Update SubscriptionManager.upgradeToPremium()**:
   - Call BillingManager to launch purchase
   - Only set premium flag after verified purchase

4. **Add Server-Side Verification**:
   - Verify purchases with Google Play Developer API
   - Prevent premium status spoofing

5. **Handle Subscription States**:
   - Active subscription
   - Grace period
   - On hold
   - Paused
   - Canceled/Expired

### Product IDs
Suggested product IDs for Google Play Console:
- `autobudget_premium_monthly` - Monthly subscription
- `autobudget_premium_yearly` - Yearly subscription (discounted)
- `autobudget_premium_lifetime` - One-time purchase (optional)

## User Experience Flow

### When Free User Hits Limit
1. User attempts restricted action (e.g., add 11th mapping)
2. Action is blocked in ViewModel
3. `showUpgradeDialog` state is set to true
4. Upgrade dialog appears with:
   - Explanation of the limit
   - Current usage information
   - List of premium benefits
5. User can either:
   - Click "Upgrade Now" → Opens upgrade flow (currently mock)
   - Click "Maybe Later" → Dismisses dialog

### After Upgrade (Mock)
1. User clicks "Upgrade Now"
2. Premium status is set to true
3. Dialog closes
4. Success message: "Successfully upgraded to Premium! 🎉"
5. All limits are removed
6. Usage counters disappear
7. User can now use all features

## Grandfathering Existing Users

**Current Behavior**: Soft enforcement
- Users who already exceed limits can keep existing data
- They just can't add more until they upgrade or remove some items
- No data is deleted when limits are exceeded

**To Implement Hard Limits** (not recommended):
- Would need migration to remove excess data
- Poor user experience
- Could lose users

## Notes

- Premium status persists across app restarts (stored in DataStore)
- No network calls required to check premium status (local-first)
- Upgrade dialog is reusable across all features
- All validation happens in ViewModel layer
- UI screens are "dumb" - they just display state and call actions

## Files Modified/Created

### New Files
1. `SubscriptionManager.kt` - Subscription logic
2. `UpgradeDialog.kt` - Upgrade prompt UI

### Modified Files
1. `HomeViewModel.kt` - Added premium checks
2. `HomeScreen.kt` - Added dialog display and testing toggle
3. `ConfigureAppsScreen.kt` - Added premium parameter and usage display
4. `ConfigureCategoryScreen.kt` - Added premium parameter and usage display
5. `AutoBudgetApplication.kt` - Added SubscriptionManager instance
6. `MainActivity.kt` - Added SubscriptionManager to ViewModel factory

## Support & Maintenance

### Adding New Premium Features
1. Add check in ViewModel before action
2. Set `showUpgradeDialog` state on denial
3. Update `UpgradeDialog` feature list if needed

### Changing Limits
Edit constants in `SubscriptionManager.kt`:
```kotlin
const val FREE_CATEGORY_MAPPINGS_LIMIT = 10  // Change as needed
const val FREE_FINANCIAL_APPS_LIMIT = 3      // Change as needed
```

### Debug Commands
Toggle premium status via Settings menu "Toggle Premium (Testing)"

---

**Implementation Date**: January 19, 2026
**Version**: 1.0
**Status**: Ready for Testing (Mock Implementation)

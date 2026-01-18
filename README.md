# AutoBudget 💰

A budget tracking Android app that automatically captures and categorizes financial transactions from notification messages and seamlessly saves them to a Google Sheet. Vibe coding experiment with Github Copilot and Claude Sonnet 4.5.

## 📋 Overview

AutoBudget simplifies personal finance management by automatically parsing transaction notifications from banking and payment apps, categorizing expenses, and syncing them to Google Sheets. No more manual entry - just spend, and let AutoBudget track it for you.

## ✨ Features

### Core Functionality
- **🔔 Automatic Transaction Detection**: Listens to notifications from financial apps and automatically extracts transaction details
- **💳 Multi-Bank Support**: Pre-configured support for major banks and payment apps including:
  - Chase, Wells Fargo, Bank of America, Citi
  - Capital One, Discover, American Express
  - PNC Bank (with enhanced parsing)
  - Payment apps: Venmo, PayPal, Cash App, Zelle, Google Pay
- **🎯 Smart Transaction Parsing**: Advanced regex patterns to extract:
  - Transaction amounts (multiple formats)
  - Merchant names and locations
  - Transaction types (debit/credit)
  - Transaction descriptions
- **📊 Google Sheets Integration**: Automatic syncing to your personal Google Sheets spreadsheet
  - Customizable spreadsheet and sheet tab names
  - Formats data row to match the default Monthly Budget Google Sheet template.
- **🏷️ Auto-Categorization**: Intelligent category detection based on merchant names and keywords
- **⚙️ Customizable Configuration**:
  - Add or exclude specific financial apps
  - Custom category mappings
  - Configure spreadsheet and sheet tab names

### User Interface
- **🏠 Home Screen**: View all transactions with sync status
- **🔧 App Configuration**: Manage which apps to monitor
- **📂 Category Management**: Customize category mappings and keywords
- **🔐 Google Sign-In**: Secure OAuth2 authentication
- **🌙 Material Design 3**: Modern, clean UI with Jetpack Compose

## 🏗️ Project Structure

```
AutoBudget/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/autobudget/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/           # Local data sources
│   │   │   │   │   ├── model/           # Data models
│   │   │   │   │   └── repository/      # Data repositories
│   │   │   │   ├── service/             # Background services
│   │   │   │   ├── sheets/              # Google Sheets integration
│   │   │   │   ├── ui/                  # User interface
│   │   │   │   │   ├── components/      # Reusable UI components
│   │   │   │   │   ├── screens/         # App screens
│   │   │   │   │   ├── theme/           # Material theming
│   │   │   │   │   └── viewmodel/       # ViewModels
│   │   │   │   ├── AutoBudgetApplication.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                     # Resources (layouts, strings, etc.)
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/                 # Instrumented tests
│   │   └── test/                        # Unit tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
```

## 🛠️ Technology Stack

### Core Technologies
- **Language**: Kotlin
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 35)

### Libraries & Frameworks
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library
- **Coroutines**: Kotlin Coroutines for asynchronous operations
- **Preferences**: DataStore (replacing SharedPreferences)
- **Navigation**: Jetpack Navigation Compose
- **Dependency Injection**: Manual DI via Application class

### Google Services
- **Authentication**: Google Sign-In (play-services-auth)
- **API Integration**: Google Sheets API v4
- **OAuth2**: Google API Client for Android

### Build Tools
- **Build System**: Gradle with Kotlin DSL
- **Compiler**: KSP (Kotlin Symbol Processing) for Room

## 📱 Key Components

### TransactionParser
The core parsing engine that extracts transaction details from notification text:
- Multiple regex patterns for flexible parsing
- Support for various date, amount, and merchant formats
- Configurable financial app detection
- Custom category mapping system

### TransactionNotificationListener
A `NotificationListenerService` that:
- Monitors incoming notifications from financial apps
- Filters relevant transaction notifications
- Parses transaction details using TransactionParser
- Saves transactions to local database
- Triggers automatic sync to Google Sheets

### SyncManager
Handles all synchronization with Google Sheets:
- Automatic background sync
- Manual sync trigger
- Batch processing of pending transactions
- Sync status tracking (Pending/Synced/Failed)
- Error handling and retry logic

### GoogleSheetsManager
Manages Google Sheets API operations:
- Appends transaction rows to specified spreadsheet
- Formats data: Date, Amount, Merchant, Category
- Validates spreadsheet access
- Handles API authentication

### Transaction Model
Room entity representing a financial transaction:
```kotlin
data class Transaction(
    val id: Long,
    val amount: Double,
    val description: String,
    val merchantName: String,
    val transactionType: TransactionType, // CREDIT/DEBIT/UNKNOWN
    val timestamp: Long,
    val sourceApp: String,
    val syncStatus: SyncStatus, // PENDING/SYNCED/FAILED
    val category: String
)
```

## 🚀 Getting Started

### Prerequisites
1. Android Studio (latest version)
2. Android device or emulator (API 26+)
3. Google Cloud Project with Sheets API enabled
4. Google Sheets spreadsheet for transaction logging

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AutoBudget
   ```

2. **Configure Google Sheets API**
   - Create a project in [Google Cloud Console](https://console.cloud.google.com/)
   - Enable Google Sheets API
   - Configure OAuth 2.0 credentials (Android type)
   - Add your SHA-1 fingerprint

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

4. **Grant permissions**
   - Open the app
   - Grant Notification Access permission (Settings → Notification Access → AutoBudget)
   - Sign in with Google account
   - Configure spreadsheet ID and sheet tab name

5. **Test transaction detection**
   - Receive a transaction notification from a supported bank/payment app
   - Check the app to see the parsed transaction
   - Verify sync to Google Sheets

## 🔧 Configuration

### Adding Custom Financial Apps
Navigate to Settings → Configure Apps to:
- View default financial apps
- Add custom package names
- Exclude specific apps from monitoring

### Customizing Categories
Navigate to Settings → Configure Categories to:
- Add keyword-to-category mappings
- Define custom spending categories
- Organize transaction classifications

### Google Sheets Setup
The app expects a spreadsheet with columns:
- Date (MM/dd/yyyy format)
- Amount (numeric)
- Merchant (text)
- Category (text)

## 🔒 Permissions

Required permissions:
- **INTERNET**: For API communication
- **ACCESS_NETWORK_STATE**: Check network availability
- **POST_NOTIFICATIONS**: Show sync notifications
- **BIND_NOTIFICATION_LISTENER_SERVICE**: Listen to notifications

## 📊 Data Flow

1. **Transaction Detection**
   ```
   Bank App → System Notification → NotificationListener → TransactionParser
   ```

2. **Data Storage**
   ```
   Parsed Transaction → Room Database → TransactionRepository
   ```

3. **Synchronization**
   ```
   TransactionRepository → SyncManager → GoogleSheetsManager → Google Sheets API
   ```

4. **User Interface**
   ```
   TransactionRepository → HomeViewModel → HomeScreen (Compose UI)
   ```

## 🧪 Testing

### Unit Tests (TODO)
Located in `app/src/test/`:
- Transaction parsing logic
- Category detection algorithms
- Data validation

### Instrumented Tests (TODO)
Located in `app/src/androidTest/`:
- Database operations
- UI component testing
- Integration tests

Run tests:
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

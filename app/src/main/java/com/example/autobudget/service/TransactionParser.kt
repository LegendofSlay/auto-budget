package com.example.autobudget.service

import com.example.autobudget.data.model.Transaction
import com.example.autobudget.data.model.TransactionType

/**
 * Parses notification text to extract financial transaction details.
 * Supports common patterns from banks and payment apps.
 */
class TransactionParser {

    companion object {
        // Common financial app package names
        val FINANCIAL_APPS = setOf(
            "com.chase.sig.android",           // Chase
            "com.wf.wellsfargomobile",         // Wells Fargo
            "com.bankofamerica.cashpromobile", // Bank of America
            "com.citi.citimobile",             // Citi
            "com.usaa.mobile.android.usaa",    // USAA
            "com.konylabs.capitalone",         // Capital One
            "com.discover.mobile",             // Discover
            "com.americanexpress.android.acctsvcs.us", // Amex
            "com.pnc.ecommerce.mobile",        // PNC Bank
            "com.venmo",                       // Venmo
            "com.paypal.android.p2pmobile",    // PayPal
            "com.squareup.cash",               // Cash App
            "com.google.android.apps.walletnfcrel", // Google Pay
            "com.apple.android.music",         // Apple Pay (unlikely on Android)
            "com.zellepay.zelle",              // Zelle
            "com.mand.notitest"
        )

        // Regex patterns for amount extraction
        private val AMOUNT_PATTERNS = listOf(
            """\$\s?([\d,]+\.?\d{0,2})""".toRegex(),           // $123.45 or $ 123.45
            """USD\s?([\d,]+\.?\d{0,2})""".toRegex(),          // USD 123.45
            """([\d,]+\.?\d{0,2})\s?(?:dollars?|USD)""".toRegex(RegexOption.IGNORE_CASE) // 123.45 dollars
        )

        // Regex patterns for transaction type
        private val DEBIT_KEYWORDS = listOf(
            "spent", "paid", "charged", "debited", "purchase", "purchased", "sent",
            "withdrawn", "withdrawal", "payment", "debit", "charge"
        )

        private val CREDIT_KEYWORDS = listOf(
            "received", "credited", "deposited", "refund", "cashback",
            "deposit", "credit", "added"
        )

        // Regex patterns for merchant extraction
        private val MERCHANT_PATTERNS = listOf(
            // PNC Bank specific pattern: "was used at Test YOUTH CENTER in +12345678901 USA for"
            """was\s+used\s+at\s+([A-Za-z0-9\s*/#+\-&'.]+?)\s+in\s+\+?\d+\s+(?:USA|US)""".toRegex(RegexOption.IGNORE_CASE),
            // PNC Bank specific pattern: "was used at CVS/PHARMACY #02815 in EAST BRUNSWIC USA for"
            """was\s+used\s+at\s+([A-Za-z0-9\s*/#+\-&'.]+in\s+[A-Za-z\s]+(?:USA|US)?)\s+for""".toRegex(RegexOption.IGNORE_CASE),
            // PNC Bank specific pattern: "purchase at DUNKIN #123456 Q35 in CITY USA for"
            """(?:purchase|transaction)\s+at\s+([A-Za-z0-9\s#]+in\s+[A-Za-z\s]+(?:USA|US)?)\s+for""".toRegex(RegexOption.IGNORE_CASE),
            """(?:at|to|from|@)\s+([A-Za-z0-9\s&'.\-]+?)(?:\.|,|\s+on|\s+for|\s*$)""".toRegex(RegexOption.IGNORE_CASE),
            """(?:merchant|store|shop):\s*([A-Za-z0-9\s&'.\-]+)""".toRegex(RegexOption.IGNORE_CASE)
        )

        // Keyword to category mapping
        private val CATEGORY_KEYWORDS = mapOf(
            "dunkin" to "Coffee/Snacks",
            "fuel" to "Transportation",
            "paris baguette" to "Coffee/Snacks",
            "cvs/pharmacy" to "Health/Medical",
            "tello" to "Rent/Utilities",
            "target" to "Home",
            "wal-mart" to "Food",
            "walmart" to "Food",
            "shoprite" to "Food",
            "bereket" to "Food",
            "nur halal meat" to "Food",
            "staples" to "Home",
            "dollar tree" to "Home",
            "google" to "Personal",
            "apple.com" to "Personal",
            "amazon" to "Home",
            "cafe" to "Coffee/Snacks",
            "expedia" to "Travel",
            "cinnabon" to "Coffee/Snacks",
            "plymouth rock" to "Rent/Utilities",
            "orthodontics" to "Health/Medical",
            "safwa" to "Coffee/Snacks",
            "aldi" to "Food",
            "jcpenney" to "Home",
            "ny times" to "Personal"
        )
    }

    /**
     * Checks if the given package name belongs to a financial app
     */
    fun isFinancialApp(packageName: String): Boolean {
        return FINANCIAL_APPS.contains(packageName) ||
               packageName.contains("bank", ignoreCase = true) ||
               packageName.contains("pay", ignoreCase = true) ||
               packageName.contains("wallet", ignoreCase = true) ||
               packageName.contains("finance", ignoreCase = true) ||
               packageName.contains("money", ignoreCase = true)
    }

    /**
     * Parses notification text to extract transaction details
     * @return Transaction if parsing successful, null otherwise
     */
    fun parseNotification(
        title: String?,
        text: String?,
        packageName: String
    ): Transaction? {
        val combinedText = listOfNotNull(title, text).joinToString(" ")

        if (combinedText.isBlank()) return null

        // Try to extract amount
        val amount = extractAmount(combinedText) ?: return null

        // Determine transaction type
        val transactionType = determineTransactionType(combinedText)

        // Extract merchant name
        val merchantName = extractMerchant(combinedText) ?: "Unknown"

        // Create description from original text (trimmed)
        val description = combinedText.take(200)

        // Detect category based on description
        val category = detectCategory(description)

        return Transaction(
            amount = amount,
            description = description,
            merchantName = merchantName,
            transactionType = transactionType,
            sourceApp = packageName,
            category = category
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun determineTransactionType(text: String): TransactionType {
        val lowerText = text.lowercase()

        for (keyword in DEBIT_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return TransactionType.DEBIT
            }
        }

        for (keyword in CREDIT_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return TransactionType.CREDIT
            }
        }

        return TransactionType.UNKNOWN
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim().take(50)
            }
        }

        // If no pattern matched, try to extract substring until the dollar amount
        val dollarIndex = text.indexOf('$')
        if (dollarIndex > 0) {
            return text.substring(0, dollarIndex).trim().take(50).ifBlank { null }
        }

        return text
    }

    private fun detectCategory(description: String): String {
        val lowerDescription = description.lowercase()

        for ((keyword, category) in CATEGORY_KEYWORDS) {
            if (lowerDescription.contains(keyword)) {
                return category
            }
        }

        return "Auto Budget"
    }
}

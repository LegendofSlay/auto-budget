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
            """(?:at|to|from|@)\s+([A-Za-z0-9\s&'.\-]+?)(?:\.|,|\s+on|\s+for|\s*$)""".toRegex(RegexOption.IGNORE_CASE),
            """(?:merchant|store|shop):\s*([A-Za-z0-9\s&'.\-]+)""".toRegex(RegexOption.IGNORE_CASE)
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

        return Transaction(
            amount = amount,
            description = description,
            merchantName = merchantName,
            transactionType = transactionType,
            sourceApp = packageName
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
}

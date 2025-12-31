package com.example.autobudget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.autobudget.data.model.SyncStatus
import com.example.autobudget.data.model.Transaction
import com.example.autobudget.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Icon(
                imageVector = when (transaction.transactionType) {
                    TransactionType.CREDIT -> Icons.Default.ArrowDownward
                    TransactionType.DEBIT -> Icons.Default.ArrowUpward
                    TransactionType.UNKNOWN -> Icons.Default.QuestionMark
                },
                contentDescription = transaction.transactionType.name,
                tint = when (transaction.transactionType) {
                    TransactionType.CREDIT -> Color(0xFF4CAF50)
                    TransactionType.DEBIT -> Color(0xFFF44336)
                    TransactionType.UNKNOWN -> Color.Gray
                },
                modifier = Modifier.size(32.dp)
            )

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = transaction.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount and sync status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = currencyFormat.format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.transactionType) {
                        TransactionType.CREDIT -> Color(0xFF4CAF50)
                        TransactionType.DEBIT -> Color(0xFFF44336)
                        TransactionType.UNKNOWN -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Sync status icon
                Icon(
                    imageVector = when (transaction.syncStatus) {
                        SyncStatus.SYNCED -> Icons.Default.CloudDone
                        SyncStatus.PENDING -> Icons.Default.CloudQueue
                        SyncStatus.FAILED -> Icons.Default.CloudOff
                    },
                    contentDescription = transaction.syncStatus.name,
                    tint = when (transaction.syncStatus) {
                        SyncStatus.SYNCED -> Color(0xFF4CAF50)
                        SyncStatus.PENDING -> Color(0xFFFF9800)
                        SyncStatus.FAILED -> Color(0xFFF44336)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


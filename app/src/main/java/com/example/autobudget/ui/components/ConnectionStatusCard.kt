package com.example.autobudget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.autobudget.sheets.GoogleSignInState

@Composable
fun ConnectionStatusCard(
    signInState: GoogleSignInState,
    spreadsheetName: String?,
    notificationListenerEnabled: Boolean,
    pendingCount: Int,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSetupNotificationsClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Google Account Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (signInState) {
                        is GoogleSignInState.SignedIn -> Icons.Default.CheckCircle
                        else -> Icons.Default.Error
                    },
                    contentDescription = "Google Status",
                    tint = when (signInState) {
                        is GoogleSignInState.SignedIn -> Color(0xFF4CAF50)
                        else -> Color(0xFFF44336)
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Account",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when (signInState) {
                            is GoogleSignInState.SignedIn -> signInState.email
                            is GoogleSignInState.Error -> signInState.message
                            else -> "Not connected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (signInState is GoogleSignInState.SignedIn) {
                    OutlinedButton(onClick = onSignOutClick) {
                        Text("Sign Out")
                    }
                } else {
                    Button(onClick = onSignInClick) {
                        Text("Sign In")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spreadsheet Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "Spreadsheet",
                    tint = if (spreadsheetName != null) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Sheet",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = spreadsheetName ?: "Not configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notification Listener Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (notificationListenerEnabled)
                        Icons.Default.Notifications
                    else
                        Icons.Default.NotificationsOff,
                    contentDescription = "Notifications",
                    tint = if (notificationListenerEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Access",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (notificationListenerEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!notificationListenerEnabled) {
                    Button(onClick = onSetupNotificationsClick) {
                        Text("Enable")
                    }
                }
            }

            if (pendingCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSyncClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync $pendingCount Pending Transactions")
                }
            }
        }
    }
}


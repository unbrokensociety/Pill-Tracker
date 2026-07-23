package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Privacy Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Privacy Policy & Security Standards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "PillTracker Cloud & Health Data Protection (GDPR & HIPAA)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EnglishPolicyContent()
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "I Accept & Understand",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EnglishPolicyContent() {
    PolicySection(
        title = "1. Governance & Regulatory Compliance",
        body = "PillTracker Health System operates in full compliance with the General Data Protection Regulation (GDPR - EU 2016/679) and Health Insurance Portability and Accountability Act (HIPAA) standards for electronic personal health records (ePHR)."
    )
    PolicySection(
        title = "2. User Authentication & Identity Management",
        body = "When authenticating via Google Sign-In or Email/Password, PillTracker processes your Unique User Identifier (UID), email address, and profile details provided by Google Identity Services. Sensitive credentials are managed strictly by Google Firebase Infrastructure and are never stored on external unencrypted servers."
    )
    PolicySection(
        title = "3. Real-Time Cloud Audit Logging",
        body = "To guarantee account integrity and security monitoring, authentication events are registered in an immutable Cloud Firestore audit collection ('users/{uid}/login_history'). Log entries record timestamp, authentication provider, application version, and hardware device metadata."
    )
    PolicySection(
        title = "4. Encryption & Cloud Storage",
        body = "Medication schedules, dose logs, and health history are transmitted using TLS 1.3 encryption and stored securely in Cloud Firestore using AES-256 bit hardware-backed encryption."
    )
    PolicySection(
        title = "5. Data Ownership & Erasure Rights",
        body = "You retain full legal ownership of your health records. You may trigger a complete account data wipe or request database export at any time through the application settings menu."
    )
    PolicySection(
        title = "6. Third-Party Non-Disclosure Guarantee",
        body = "PillTracker strictly prohibits selling, renting, or sharing personal health records or login audit trails with third-party brokers, advertisers, or analytics vendors."
    )
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

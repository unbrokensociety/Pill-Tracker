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
        title = "1. Legal Agreement & Binding Express Consent",
        body = "By checking the 'I Agree' box and completing the registration process in PillTracker, you explicitly acknowledge, agree to, and enter into a legally binding contract governed by applicable digital privacy laws, including the European Union General Data Protection Regulation (GDPR Article 6(1)(a)) and the California Consumer Privacy Act (CCPA). Your affirmative consent, along with the timestamp, account identifier, and hardware device fingerprint, is recorded in an immutable cloud audit database as proof of agreement."
    )
    PolicySection(
        title = "2. Medical Disclaimer & Non-Medical Advice Statement",
        body = "PillTracker is an administrative medication tracking tool and organizational utility ONLY. PillTracker is NOT a medical device, diagnostic tool, or substitute for professional medical advice, diagnosis, treatment, or clinical judgment. Never disregard professional medical advice or delay seeking medical care because of information or notifications provided by PillTracker. The user assumes full and sole responsibility for verifying medication names, dosages, intake times, and doctor instructions."
    )
    PolicySection(
        title = "3. Assumption of Risk & Comprehensive Limitation of Liability",
        body = "TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, PILLTRACKER, ITS DEVELOPERS, AFFILIATES, AND SERVICE PROVIDERS SHALL NOT BE HELD LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL, PUNITIVE, OR ADVERSE HEALTH CONSEQUENCES, INJURIES, OR DAMAGES ARISING OUT OF OR IN CONNECTION WITH: (A) MISSED, DELAYED, INACCURATE, OR DUPLICATE MEDICATION DOSES; (B) SYSTEM NOTIFICATION DELAYS CAUSED BY OS BATTERY OPTIMIZATIONS, DEVICE REBOOTS, OR HARDWARE LIMITATIONS; (C) USER ENTRY ERRORS OR INACCURATE SCHEDULE INPUTS; OR (D) TEMPORARY UNAVAILABILITY OF CLOUD SYNC SERVICES."
    )
    PolicySection(
        title = "4. User Indemnification & Hold Harmless Agreement",
        body = "You agree to defend, indemnify, and hold harmless PillTracker, its developers, operators, and hosting partners from and against any claims, liabilities, damages, losses, costs, or expenses (including reasonable attorney fees) arising from or related to your misuse of the application, failure to follow prescribed medical regimens, or breach of these terms."
    )
    PolicySection(
        title = "5. Identity Verification & Cloud Security Architecture",
        body = "User accounts created via Email/Password utilize Google Firebase Authentication services. Verification emails contain unique encrypted activation tokens sent directly to your registered email address. Authentication events, consent records, and encrypted health records are secured using AES-256 bit hardware-backed encryption in transit (TLS 1.3) and at rest in Google Cloud Firestore infrastructure."
    )
    PolicySection(
        title = "6. Data Subject Rights & Absolute Erasure Guarantee",
        body = "In compliance with GDPR and HIPAA ePHR regulations, you retain complete legal ownership of your health logs. You hold the right to inspect, export, or permanently purge all personal health logs and cloud audit records from our systems at any time via the settings menu ('Delete Account / Reset Data')."
    )
    PolicySection(
        title = "7. Non-Disclosure & Anti-Commercialization Pledge",
        body = "PillTracker strictly prohibits selling, renting, licensing, or sharing your personal health records, email addresses, or usage logs with any third-party advertisers, data brokers, insurance agencies, or commercial analytics platforms."
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

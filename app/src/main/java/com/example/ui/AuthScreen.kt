package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlassCard
import com.google.firebase.auth.FirebaseAuth

enum class AuthTab { SIGN_IN, SIGN_UP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onCompleteAuth: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(AuthTab.SIGN_IN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsOfService by remember { mutableStateOf(false) }

    // Email Verification state
    var showVerificationDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showGuestTermsDialog by remember { mutableStateOf(false) }
    var guestTermsAccepted by remember { mutableStateOf(false) }
    var guestTermsError by remember { mutableStateOf<String?>(null) }
    var privacyAccepted by remember { mutableStateOf(false) }
    var privacyError by remember { mutableStateOf<String?>(null) }
    var pendingAccountName by remember { mutableStateOf("") }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val primaryColor = MaterialTheme.colorScheme.primary

    // Auto-polling for email link verification
    LaunchedEffect(showVerificationDialog) {
        while (showVerificationDialog) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.reload().addOnCompleteListener { task ->
                        if (task.isSuccessful && user.isEmailVerified) {
                            viewModel.loginOrRegister(
                                name = pendingAccountName,
                                email = pendingEmail,
                                provider = "EMAIL",
                                passwordHash = pendingPassword
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.account_created_message, pendingAccountName),
                                Toast.LENGTH_LONG
                            ).show()
                            showVerificationDialog = false
                            onCompleteAuth()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(2500)
        }
    }

    if (showPrivacyPolicy) {
        com.example.ui.components.PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicy = false }
        )
    }

    if (showTermsOfService) {
        com.example.ui.components.TermsOfServiceDialog(
            onDismiss = { showTermsOfService = false }
        )
    }

    if (showGuestTermsDialog) {
        AlertDialog(
            onDismissRequest = { showGuestTermsDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Умови для гостьового режиму",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Для продовження роботи в гостьовому режимі без реєстрації акаунту необхідно підтвердити згоду з Умовами використання та Політикою конфіденційності.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "• Усі дані ліків зберігаються лише на цьому пристрої.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Хмарна резервна копія вимкнена до створення акаунту.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                guestTermsAccepted = !guestTermsAccepted
                                guestTermsError = null
                            }
                    ) {
                        Checkbox(
                            checked = guestTermsAccepted,
                            onCheckedChange = {
                                guestTermsAccepted = it
                                guestTermsError = null
                            }
                        )
                        Text(
                            text = "Я приймаю Умови використання та Політику конфіденційності",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (guestTermsError != null) {
                        Text(
                            text = guestTermsError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showPrivacyPolicy = true }) {
                            Text("Політика", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = { showTermsOfService = true }) {
                            Text("Умови", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!guestTermsAccepted) {
                            guestTermsError = "Будь ласка, погодьтеся з умовами для продовження"
                            return@Button
                        }
                        showGuestTermsDialog = false
                        viewModel.skipAuthAsGuest()
                        onCompleteAuth()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Продовжити як гість", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestTermsDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                primaryColor.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.auth_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.auth_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Auth Form Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Primary Auth Tab Switcher (Sign In vs Register)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        AuthTab.values().forEach { tab ->
                            val isSelected = (activeTab == tab)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable {
                                        activeTab = tab
                                        emailError = null
                                        passwordError = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tab == AuthTab.SIGN_IN) stringResource(R.string.auth_tab_signin) else stringResource(R.string.auth_tab_signup),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Input fields
                    AnimatedVisibility(
                        visible = activeTab == AuthTab.SIGN_UP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.auth_field_name)) },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        label = { Text(stringResource(R.string.auth_field_email)) },
                        leadingIcon = { Icon(Icons.Filled.Email, null) },
                        isError = emailError != null,
                        supportingText = emailError?.let { err -> { Text(err) } },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text(stringResource(R.string.auth_field_password)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { err -> { Text(err) } },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    // Forgot Password Button for SIGN_IN
                    AnimatedVisibility(
                        visible = activeTab == AuthTab.SIGN_IN,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = { showForgotPasswordDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_forgot_password),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Mandatory Privacy Policy Consent Checkbox for Registration
                    AnimatedVisibility(
                        visible = activeTab == AuthTab.SIGN_UP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        privacyAccepted = !privacyAccepted
                                        if (privacyAccepted) privacyError = null
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = privacyAccepted,
                                    onCheckedChange = {
                                        privacyAccepted = it
                                        if (it) privacyError = null
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.auth_privacy_checkbox),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            privacyError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }

                    // Email/Password Submit Button
                    val invalidEmailMsg = stringResource(R.string.auth_error_invalid_email)
                    val shortPassMsg = stringResource(R.string.auth_error_short_password)
                    val privacyReqMsg = stringResource(R.string.auth_privacy_required_error)
                    val invalidCredsMsg = stringResource(R.string.auth_invalid_credentials)

                    Button(
                        onClick = {
                            if (!email.contains("@") || !email.contains(".")) {
                                emailError = invalidEmailMsg
                                return@Button
                            }
                            if (password.length < 6) {
                                passwordError = shortPassMsg
                                return@Button
                            }
                            if (activeTab == AuthTab.SIGN_UP && !privacyAccepted) {
                                privacyError = privacyReqMsg
                                return@Button
                            }

                            val accountName = if (name.isNotBlank()) name else email.substringBefore("@")
                            
                            if (activeTab == AuthTab.SIGN_UP) {
                                pendingAccountName = accountName
                                pendingEmail = email
                                pendingPassword = password
                                isLoading = true

                                try {
                                    val auth = FirebaseAuth.getInstance()
                                    auth.useAppLanguage()
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener { authResult ->
                                            isLoading = false
                                            showVerificationDialog = true
                                            authResult.user?.sendEmailVerification()
                                                ?.addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        Toast.makeText(context, context.getString(R.string.verification_email_sent, email), Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            val msg = e.localizedMessage ?: "Registration error"
                                            passwordError = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                } catch (e: Exception) {
                                    isLoading = false
                                    e.printStackTrace()
                                }
                            } else {
                                isLoading = true
                                passwordError = null
                                emailError = null

                                try {
                                    val auth = FirebaseAuth.getInstance()
                                    auth.useAppLanguage()
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener { authResult ->
                                            isLoading = false
                                            val user = authResult.user
                                            val displayName = if (!user?.displayName.isNullOrBlank()) user!!.displayName!! else accountName
                                            Toast.makeText(context, context.getString(R.string.welcome_back_message, displayName), Toast.LENGTH_LONG).show()

                                            viewModel.loginOrRegister(
                                                name = displayName,
                                                email = email,
                                                provider = "EMAIL",
                                                passwordHash = password
                                            )
                                            viewModel.triggerCloudRestore { }
                                            onCompleteAuth()
                                        }
                                        .addOnFailureListener { e ->
                                            // Fallback check against saved local users for offline mode
                                            val localUser = viewModel.allSavedUsers.value.find { it.email.equals(email, ignoreCase = true) }
                                            if (localUser != null && localUser.passwordHash == password) {
                                                isLoading = false
                                                Toast.makeText(context, context.getString(R.string.welcome_back_message, localUser.name), Toast.LENGTH_LONG).show()
                                                viewModel.loginOrRegister(
                                                    name = localUser.name,
                                                    email = email,
                                                    provider = localUser.authProvider,
                                                    passwordHash = password
                                                )
                                                viewModel.triggerCloudRestore { }
                                                onCompleteAuth()
                                            } else {
                                                isLoading = false
                                                val errMsg = if (localUser != null) invalidCredsMsg else (e.localizedMessage ?: invalidCredsMsg)
                                                passwordError = errMsg
                                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } catch (e: Exception) {
                                    isLoading = false
                                    e.printStackTrace()
                                    passwordError = e.localizedMessage ?: invalidCredsMsg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (activeTab == AuthTab.SIGN_IN) stringResource(R.string.auth_btn_signin) else stringResource(R.string.auth_btn_signup),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Skip / Guest Mode
            OutlinedButton(
                onClick = {
                    showGuestTermsDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.auth_btn_skip),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_skip_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showPrivacyPolicy = true }
                ) {
                    Text(
                        text = stringResource(R.string.privacy_policy_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(" • ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = { showTermsOfService = true }
                ) {
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showVerificationDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showVerificationDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.auth_verification_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.auth_verification_desc, pendingEmail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto-verification active indicator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.auth_verification_auto_checking),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.reload()?.addOnCompleteListener { task ->
                                if (user.isEmailVerified) {
                                    viewModel.loginOrRegister(
                                        name = pendingAccountName,
                                        email = pendingEmail,
                                        provider = "EMAIL",
                                        passwordHash = pendingPassword
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.account_created_message, pendingAccountName),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showVerificationDialog = false
                                    onCompleteAuth()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.auth_verification_wrong_code),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_verification_check_link),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                            Toast.makeText(
                                context,
                                context.getString(R.string.verification_email_sent, pendingEmail),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(text = stringResource(R.string.auth_verification_resend))
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    TextButton(
                        onClick = {
                            viewModel.loginOrRegister(
                                name = pendingAccountName,
                                email = pendingEmail,
                                provider = "EMAIL",
                                passwordHash = pendingPassword
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.account_created_message, pendingAccountName),
                                Toast.LENGTH_LONG
                            ).show()
                            showVerificationDialog = false
                            onCompleteAuth()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.auth_verification_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit
) {
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.auth_reset_password_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.auth_reset_password_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text(stringResource(R.string.auth_field_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                statusMsg?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (resetEmail.contains("@") && resetEmail.contains(".")) {
                        isLoading = true
                        try {
                            val auth = FirebaseAuth.getInstance()
                            auth.useAppLanguage()
                            auth.sendPasswordResetEmail(resetEmail)
                                .addOnSuccessListener {
                                    isLoading = false
                                    val msg = context.getString(R.string.auth_reset_email_sent, resetEmail)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    val msg = e.localizedMessage ?: "Error sending reset email"
                                    statusMsg = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            statusMsg = e.localizedMessage
                        }
                    } else {
                        statusMsg = context.getString(R.string.auth_error_invalid_email)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.auth_btn_send_reset))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}


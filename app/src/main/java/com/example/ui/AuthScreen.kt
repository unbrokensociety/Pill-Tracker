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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

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

    // Email Verification Code state
    var showVerificationDialog by remember { mutableStateOf(false) }
    var enteredCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var pendingAccountName by remember { mutableStateOf("") }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val primaryColor = MaterialTheme.colorScheme.primary

    if (showPrivacyPolicy) {
        com.example.ui.components.PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicy = false }
        )
    }

    // Real Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember(context) { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val userEmail = account.email ?: "user@gmail.com"
                val userName = account.displayName ?: userEmail.substringBefore("@")
                val avatarUrl = account.photoUrl?.toString() ?: ""

                // Firebase Auth Sign-In if ID Token available
                try {
                    val auth = FirebaseAuth.getInstance()
                    auth.useAppLanguage()
                    if (account.idToken != null) {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        auth.signInWithCredential(credential)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                viewModel.loginWithGoogle(
                    email = userEmail,
                    name = userName,
                    avatarUrl = avatarUrl
                )
                Toast.makeText(context, context.getString(R.string.welcome_message, userName), Toast.LENGTH_LONG).show()
                onCompleteAuth()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Google Sign-In canceled or unavailable", Toast.LENGTH_SHORT).show()
        }
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
                    // Native Google Sign-In Button (Triggers system Google Account Chooser)
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            try {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            } catch (e: Exception) {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.auth_btn_google),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = " OR ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Tab Switcher (Sign In vs Register)
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
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tab == AuthTab.SIGN_IN) stringResource(R.string.auth_tab_signin) else stringResource(R.string.auth_tab_signup),
                                    style = MaterialTheme.typography.labelLarge,
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

                    // Email/Password Submit Button
                    val invalidEmailMsg = stringResource(R.string.auth_error_invalid_email)
                    val shortPassMsg = stringResource(R.string.auth_error_short_password)

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

                            val accountName = if (name.isNotBlank()) name else email.substringBefore("@")
                            
                            if (activeTab == AuthTab.SIGN_UP) {
                                pendingAccountName = accountName
                                pendingEmail = email
                                pendingPassword = password
                                enteredCode = ""
                                codeError = null
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
                                try {
                                    val auth = FirebaseAuth.getInstance()
                                    auth.useAppLanguage()
                                    auth.signInWithEmailAndPassword(email, password)
                                    Toast.makeText(context, context.getString(R.string.welcome_back_message, accountName), Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                viewModel.loginOrRegister(
                                    name = accountName,
                                    email = email,
                                    provider = "EMAIL",
                                    passwordHash = password
                                )
                                onCompleteAuth()
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
                    viewModel.skipAuthAsGuest()
                    onCompleteAuth()
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

            TextButton(
                onClick = { showPrivacyPolicy = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.privacy_policy_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
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

                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = {
                            enteredCode = it
                            codeError = null
                        },
                        label = { Text(stringResource(R.string.auth_verification_code_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        isError = codeError != null,
                        supportingText = codeError?.let { err -> { Text(err) } },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val trimmed = enteredCode.trim()
                            if (trimmed.isNotBlank()) {
                                // Try applying action code from email link via Firebase Auth
                                FirebaseAuth.getInstance().applyActionCode(trimmed)
                                    .addOnSuccessListener {
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
                                    .addOnFailureListener {
                                        // If applyActionCode failed, check if user is already verified via link
                                        val user = FirebaseAuth.getInstance().currentUser
                                        user?.reload()?.addOnCompleteListener {
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
                                                codeError = context.getString(R.string.auth_verification_wrong_code)
                                            }
                                        }
                                    }
                            } else {
                                codeError = context.getString(R.string.auth_verification_wrong_code)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_verification_confirm),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = stringResource(R.string.auth_verification_check_link))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

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
                        Text(text = stringResource(R.string.auth_verification_skip))
                    }

                    Spacer(modifier = Modifier.height(2.dp))

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
                }
            }
        }
    }
}


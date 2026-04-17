package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.AuthState
import com.example.myapplication.AuthViewModel
import com.example.myapplication.R
import com.example.myapplication.data.AiExpenseParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPage(
    authViewModel: AuthViewModel = viewModel(),
    autoSyncEnabled: Boolean,
    onAutoSyncToggle: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    isSyncing: Boolean = false,
    syncMessage: String = "",
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val resetEmailSentText = stringResource(R.string.auth_reset_email_sent)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var redeemCode by remember { mutableStateOf("") }
    var redeemLoading by remember { mutableStateOf(false) }
    var redeemMessage by remember { mutableStateOf("") }
    var usageStatus by remember { mutableStateOf<com.example.myapplication.data.AiExpenseParser.UsageStatus?>(null) }
    var isLoadingUsage by remember { mutableStateOf(false) }

    val userPlan by authViewModel.userPlan.collectAsStateWithLifecycle()
    val planExpiresAt by authViewModel.planExpiresAt.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        isLoadingUsage = true
        AiExpenseParser.fetchUsageStatus().onSuccess { usageStatus = it }
        isLoadingUsage = false
    }

    val userEmail = when (val state = authState) {
        is AuthState.Authenticated -> state.user.email
        is AuthState.EmailNotVerified -> state.user.email
        else -> null
    } ?: stringResource(R.string.account_email_placeholder)
    val avatarText = userEmail.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarText,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    val expiresLabel = planExpiresAt?.take(10)?.let { dateStr ->
                        try {
                            val parsed = java.time.LocalDate.parse(dateStr)
                            val formatter = java.time.format.DateTimeFormatter.ofPattern(
                                "MMM d, yyyy",
                                java.util.Locale.getDefault()
                            )
                            parsed.format(formatter)
                        } catch (_: Exception) {
                            dateStr
                        }
                    } ?: ""
                    val planLabel = if (userPlan == "pro") {
                        stringResource(R.string.account_plan_pro_expires, expiresLabel)
                    } else {
                        stringResource(R.string.account_level_free)
                    }
                    val planColor = if (userPlan == "pro")
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                    val planTextColor = if (userPlan == "pro")
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant

                    Surface(shape = MaterialTheme.shapes.small, color = planColor) {
                        Text(
                            text = planLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = planTextColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_storage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.account_storage_usage),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.account_storage_limit_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_ai_usage_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isLoadingUsage) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        val status = usageStatus
                        if (status == null) {
                            Text(
                                text = stringResource(R.string.account_ai_usage_load_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Parse usage row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.account_ai_usage_parse_label),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (status.parseLimit == null)
                                            "${status.parseUsed} / ${context.getString(R.string.account_ai_usage_unlimited)}"
                                        else "${status.parseUsed} / ${status.parseLimit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (status.parseLimit != null && status.parseUsed >= status.parseLimit)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (status.parseLimit != null) {
                                    LinearProgressIndicator(
                                        progress = { (status.parseUsed.toFloat() / status.parseLimit).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (status.parseUsed >= status.parseLimit)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Analyze usage row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.account_ai_usage_analyze_label),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (status.analyzeLimit == null)
                                            "${status.analyzeUsed} / ${context.getString(R.string.account_ai_usage_unlimited)}"
                                        else "${status.analyzeUsed} / ${status.analyzeLimit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (status.analyzeLimit != null && status.analyzeUsed >= status.analyzeLimit)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (status.analyzeLimit != null) {
                                    LinearProgressIndicator(
                                        progress = { (status.analyzeUsed.toFloat() / status.analyzeLimit).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (status.analyzeUsed >= status.analyzeLimit)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (status.plan == "free") {
                                Text(
                                    text = stringResource(R.string.account_ai_usage_upgrade_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_backup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = { onSyncNow() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.account_manual_backup))
                    }

                    if (isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (syncMessage.isNotEmpty()) {
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.account_auto_backup),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.account_auto_sync_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = onAutoSyncToggle
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.account_actions_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Activate Pro") },
                        supportingContent = { Text("Enter an activation code to unlock Pro features") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRedeemDialog = true }
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.account_change_password)) },
                        supportingContent = { Text(stringResource(R.string.account_change_password_desc)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                authViewModel.sendPasswordResetEmail(userEmail)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message = resetEmailSentText)
                                }
                            }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.account_sign_out)) },
                        supportingContent = { Text(stringResource(R.string.account_sign_out_desc)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSignOutDialog = true }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.account_delete_account),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.account_delete_account_desc),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirmDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.account_delete_account)) },
            text = {
                Text(
                    stringResource(R.string.account_delete_account_warning)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        authViewModel.deleteAccount()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.account_sign_out)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.account_sign_out_choose_mode))
                    Text(
                        stringResource(R.string.account_sign_out_options_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            authViewModel.signOutWithClearData(clearLocal = false)
                        }
                    ) {
                        Text(stringResource(R.string.account_keep_local_data))
                    }
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            authViewModel.signOutWithClearData(clearLocal = true)
                        }
                    ) {
                        Text(stringResource(R.string.account_clear_local_data))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showRedeemDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!redeemLoading) {
                    showRedeemDialog = false
                    redeemCode = ""
                    redeemMessage = ""
                }
            },
            title = { Text("Activate Pro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = redeemCode,
                        onValueChange = { redeemCode = it.uppercase() },
                        label = { Text("Activation Code") },
                        placeholder = { Text("SMART-XXXX-XXXX-XXXX") },
                        singleLine = true,
                        enabled = !redeemLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (redeemMessage.isNotEmpty()) {
                        Text(
                            text = redeemMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (redeemMessage.startsWith("✓"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    if (redeemLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        redeemLoading = true
                        redeemMessage = ""
                        coroutineScope.launch {
                            val result = com.example.myapplication.data.AiExpenseParser.redeemCode(redeemCode.trim())
                            result.onSuccess { status ->
                                authViewModel.refreshSubscriptionStatus()
                                redeemMessage = "✓ Pro activated! Expires ${status.expiresAt?.take(10) ?: ""}"
                            }
                            result.onFailure { e ->
                                redeemMessage = e.message ?: "Redemption failed"
                            }
                            redeemLoading = false
                        }
                    },
                    enabled = redeemCode.isNotBlank() && !redeemLoading
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!redeemLoading) {
                            showRedeemDialog = false
                            redeemCode = ""
                            redeemMessage = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

private fun AuthViewModel.deleteAccount() {
    // Placeholder until delete-account behavior is implemented in AuthViewModel.
}



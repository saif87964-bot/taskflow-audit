package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.security.BiometricHelper
import com.taskflow.audit.security.BiometricResult
import com.taskflow.audit.ui.viewmodel.DirectoryEntry
import com.taskflow.audit.ui.viewmodel.LoginViewModel

// Fallback shown only before the first successful login caches the live directory
private val fallbackStaffList = listOf(
    DirectoryEntry("im", "Imtiaz M", "Senior Auditor", "#1565C0"),
    DirectoryEntry("kh", "Khalid H", "Audit Manager",  "#00695C"),
    DirectoryEntry("av", "Anita V",  "Auditor",         "#6A1B9A"),
    DirectoryEntry("an", "Amina N",  "Tax Consultant",  "#E65100"),
    DirectoryEntry("jp", "James P",  "Partner",         "#37474F"),
)

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { Color(0xFF1565C0) }

@Composable
fun LoginScreen(
    onLogin: (staffUid: String, isAdmin: Boolean) -> Unit,
    onPinChange: (uid: String, isAdmin: Boolean) -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Navigate on successful auth
    LaunchedEffect(state.navigateToStaffHome, state.navigateToAdmin) {
        state.navigateToStaffHome?.let { uid ->
            vm.clearNavigation()
            onLogin(uid, false)
        }
        state.navigateToAdmin?.let { uid ->
            vm.clearNavigation()
            onLogin(uid, true)
        }
    }

    // Navigate to PIN change when flagged
    LaunchedEffect(state.navigateToPinChange) {
        state.navigateToPinChange?.let { uid ->
            vm.clearNavigation()
            onPinChange(uid, state.pendingIsAdmin)
        }
    }

    // Biometric enrollment dialog after first PIN login
    if (state.offerBiometricEnroll && BiometricHelper.isAvailable(context)) {
        AlertDialog(
            onDismissRequest = { vm.skipBiometricAndProceed() },
            icon = { Icon(Icons.Default.Fingerprint, null) },
            title = { Text("Enable Fingerprint Unlock?") },
            text = { Text("Sign in faster next time using your fingerprint or face. You can always use your PIN instead.") },
            confirmButton = {
                Button(onClick = { vm.enableBiometricAndProceed() }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { vm.skipBiometricAndProceed() }) { Text("Not Now") }
            }
        )
    } else if (state.offerBiometricEnroll) {
        // Biometric not available — skip enrollment silently
        LaunchedEffect(Unit) { vm.skipBiometricAndProceed() }
    }

    // Auto-prompt biometric for returning users
    LaunchedEffect(state.requiresBiometric) {
        if (state.requiresBiometric && activity != null && BiometricHelper.isAvailable(context)) {
            BiometricHelper.prompt(activity) { result ->
                when (result) {
                    is BiometricResult.Success -> vm.onBiometricSuccess()
                    is BiometricResult.UserCancelled -> vm.onBiometricDismissed()
                    is BiometricResult.Error -> vm.onBiometricDismissed()
                }
            }
        }
    }

    val staffList = state.directory.ifEmpty { fallbackStaffList }

    var selectedStaff by remember { mutableStateOf<DirectoryEntry?>(null) }
    var pinEntry by remember { mutableStateOf("") }

    // Clear the PIN dots after a failed attempt so the keypad is usable again
    LaunchedEffect(state.error) {
        if (state.error != null) pinEntry = ""
    }

    val onAvatarClick = { s: DirectoryEntry ->
        selectedStaff = s
        pinEntry = ""
        vm.clearError()
    }

    val onKeyPress = { digit: String ->
        if (pinEntry.length < 4) {
            pinEntry += digit
            if (pinEntry.length == 4) {
                selectedStaff?.let { vm.signIn(it.shortId, pinEntry) }
            }
        }
    }

    val onDelete = {
        if (pinEntry.isNotEmpty()) pinEntry = pinEntry.dropLast(1)
    }

    // Surface (not raw background) so LocalContentColor follows the theme —
    // otherwise headings render black even in dark mode
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        // Brand icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("TF", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "TaskFlow Audit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "TIME TRACKING & ENGAGEMENT MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(40.dp))
        Text(
            "SELECT STAFF",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(14.dp))

        // Staff avatars — wraps onto extra rows when the team grows
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            staffList.forEach { s ->
                val isSelected = selectedStaff?.shortId == s.shortId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAvatarClick(s) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(parseColor(s.colorHex).copy(alpha = if (isSelected) 1f else 0.35f))
                            .then(
                                if (isSelected)
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s.shortId.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.shortId.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (selectedStaff != null) {
            Text(selectedStaff!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                selectedStaff!!.role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < pinEntry.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    state.error!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                PinKeypad(onKey = onKeyPress, onDelete = onDelete)

                // Biometric shortcut for returning users
                if (BiometricHelper.isAvailable(context) && AppRepositories.auth.isBiometricEnabled()) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        activity?.let { a ->
                            BiometricHelper.prompt(a) { result ->
                                if (result is BiometricResult.Success) vm.onBiometricSuccess()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Use biometric")
                    }
                }
            }
        } else {
            Text(
                "Tap your avatar to sign in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

@Composable
private fun PinKeypad(onKey: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(keys.size) { i ->
            val key = keys[i]
            if (key.isEmpty()) {
                Box(Modifier.aspectRatio(1.8f))
            } else {
                Box(
                    modifier = Modifier
                        .aspectRatio(1.8f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { if (key == "⌫") onDelete() else onKey(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

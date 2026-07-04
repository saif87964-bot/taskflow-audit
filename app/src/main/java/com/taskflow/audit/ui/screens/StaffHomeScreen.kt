package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.audit.ui.components.EngagementChip
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.viewmodel.StaffHomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    vm: StaffHomeViewModel,
    onNavigateToTimesheet: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLogbook: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showEngagementSheet by remember { mutableStateOf(false) }

    // Running timer display for active session
    var elapsedDisplay by remember { mutableStateOf("") }
    LaunchedEffect(state.activeSession) {
        val session = state.activeSession
        if (session != null) {
            while (true) {
                val startSec = session.startTime?.seconds ?: (System.currentTimeMillis() / 1000)
                val elapsed = ((System.currentTimeMillis() / 1000) - startSec).toInt()
                elapsedDisplay = "%dh %02dm".format(elapsed / 3600, (elapsed % 3600) / 60)
                kotlinx.coroutines.delay(60_000)
            }
        } else {
            elapsedDisplay = ""
        }
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearError()
        }
    }

    val today = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date())
    val firstName = state.staff?.fullName?.split(" ")?.first() ?: ""

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (onBack != null) "My Day" else "TaskFlow Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(today, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Admin Hub")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    // When opened from the Admin Hub, back handles the exit — no logout here
                    if (onBack == null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Greeting hero — gradient brand card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "IMH ASSOCIATES",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (firstName.isNotEmpty()) "Hello, $firstName" else "Welcome back",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = state.staff?.role ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.1f".format(state.hoursToday),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "hrs today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Check-in / Check-out FAB
            com.taskflow.audit.ui.components.CheckInFab(
                isCheckedIn = state.isCheckedIn,
                elapsedTime = elapsedDisplay,
                onClick = {
                    if (!state.isCheckedIn) {
                        showEngagementSheet = true
                    } else {
                        vm.checkOut()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Current engagement card
            if (state.isCheckedIn && state.currentEngagement != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CheckedInGreen.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CURRENT ENGAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CheckedInGreen,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        Spacer(Modifier.height(8.dp))
                        EngagementChip(doc = state.currentEngagement!!, selected = true)
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { showEngagementSheet = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Switch Engagement")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStatCard(
                    label = "Sessions",
                    value = "${state.todaySessions.size}",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    label = "Engagements",
                    value = "${state.todaySessions.map { it.engagementId }.distinct().size}",
                    icon = Icons.Default.BusinessCenter,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    label = "Tasks Pending",
                    value = "${state.pendingTaskCount}",
                    icon = Icons.Default.CheckBox,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToTimesheet,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Timesheet", style = MaterialTheme.typography.labelMedium)
                }
                BadgedBox(
                    badge = {
                        if (state.pendingTaskCount > 0) Badge { Text("${state.pendingTaskCount}") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToTasks,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tasks", style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(
                    onClick = onNavigateToLogbook,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Logbook", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showEngagementSheet) {
        EngagementSelectorSheet(
            engagements = state.engagements,
            onDismiss = { showEngagementSheet = false },
            onSelect = { engagementId ->
                if (state.isCheckedIn) vm.switchEngagement(engagementId)
                else vm.checkIn(engagementId)
                showEngagementSheet = false
            }
        )
    }
}

@Composable
private fun QuickStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(12.dp, 14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

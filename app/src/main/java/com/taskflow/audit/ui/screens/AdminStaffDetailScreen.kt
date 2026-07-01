package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.data.model.TimeSessionDocument
import com.taskflow.audit.ui.components.WorkloadProgressBar
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.viewmodel.AdminStaffDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffDetailScreen(
    vm: AdminStaffDetailViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.pinResetSuccess) {
        if (state.pinResetSuccess) {
            snackbarHostState.showSnackbar("PIN reset. Staff will be prompted on next login.")
            vm.clearMessages()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessages()
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset PIN to 1234?") },
            text = { Text("The staff member will be prompted to set a new PIN on their next login.") },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    vm.resetPin()
                }) { Text("Reset PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val staff = state.staff
                    if (staff != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val avatarColor = try {
                                Color(android.graphics.Color.parseColor(staff.colorHex))
                            } catch (_: Exception) { Color(0xFF1565C0) }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor)
                            ) {
                                Text(staff.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(staff.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(staff.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text("Staff Detail")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "Reset PIN")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Status card
            item {
                val currentEngName = state.currentEngagementId?.let { engId ->
                    state.engagements.find { it.id == engId }?.clientName
                }
                val currentEngColor = state.currentEngagementId?.let { engId ->
                    state.engagements.find { it.id == engId }?.colorHex
                }?.let {
                    try { Color(android.graphics.Color.parseColor(it)) }
                    catch (_: Exception) { CheckedInGreen }
                } ?: CheckedInGreen

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isCheckedIn)
                            CheckedInGreen.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (state.isCheckedIn) "Currently Active" else "Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentEngName != null) {
                                Text(
                                    currentEngName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = currentEngColor
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "%.1fh".format(state.hoursToday),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Workload by engagement
            val breakdown = state.todaySessions
                .groupBy { it.engagementId }
                .mapValues { (_, list) -> list.sumOf { it.durationMinutes } / 60f }

            if (breakdown.isNotEmpty()) {
                item {
                    Text(
                        "TIME BY ENGAGEMENT TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        breakdown.forEach { (engId, hours) ->
                            val eng = state.engagements.find { it.id == engId } ?: return@forEach
                            val engColor = try {
                                Color(android.graphics.Color.parseColor(eng.colorHex))
                            } catch (_: Exception) { CheckedInGreen }
                            WorkloadProgressBar(
                                label = eng.clientName,
                                hoursUsed = hours,
                                hoursBudget = eng.budgetHours.toFloat(),
                                color = engColor
                            )
                        }
                    }
                }
            }

            // Timeline
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "TODAY'S TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }

            if (state.todaySessions.isEmpty()) {
                item {
                    Text(
                        "No sessions logged today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(state.todaySessions) { session ->
                    val engName = state.engagements.find { it.id == session.engagementId }?.clientName ?: "Unknown"
                    SessionDocRow(session = session, engagementName = engName)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SessionDocRow(session: TimeSessionDocument, engagementName: String) {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startLabel = session.startTime?.toDate()?.let { timeFmt.format(it) } ?: "--:--"
    val endLabel = if (session.isActive) "Active" else session.endTime?.toDate()?.let { timeFmt.format(it) } ?: "--:--"
    val durationLabel = if (session.durationMinutes > 0) {
        val h = session.durationMinutes / 60
        val m = session.durationMinutes % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    } else if (session.isActive) {
        "Ongoing"
    } else {
        "0m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(engagementName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "$startLabel → $endLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (session.isActive) CheckedInGreen.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (session.isActive) CheckedInGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

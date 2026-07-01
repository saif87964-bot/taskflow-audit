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
import com.taskflow.audit.data.mock.Engagement
import com.taskflow.audit.data.mock.MockData
import com.taskflow.audit.data.mock.StaffStatus
import com.taskflow.audit.data.mock.TaskStatus
import com.taskflow.audit.ui.components.CheckInFab
import com.taskflow.audit.ui.components.EngagementChip
import com.taskflow.audit.ui.theme.CheckedInGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    staffId: String,
    onNavigateToTimesheet: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLogbook: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var status by remember { mutableStateOf(MockData.getStaffStatus(staffId)!!) }
    var showEngagementSheet by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf("0h 00m") }
    val myTasks = remember(staffId) { MockData.getTasksForStaff(staffId) }
    val pendingTaskCount = myTasks.count { it.status != TaskStatus.DONE }

    // Simulate timer ticking
    LaunchedEffect(status.isCheckedIn) {
        if (status.isCheckedIn) {
            var seconds = 5400 // mock 1.5h elapsed
            while (status.isCheckedIn) {
                elapsedTime = "%dh %02dm".format(seconds / 3600, (seconds % 3600) / 60)
                kotlinx.coroutines.delay(60_000)
                seconds += 60
            }
        }
    }

    val today = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TaskFlow Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(today, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Greeting card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${status.staff.fullName.split(" ").first()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = status.staff.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    // Hours today badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1f".format(status.hoursToday),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "hrs today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Check-in FAB
            CheckInFab(
                isCheckedIn = status.isCheckedIn,
                elapsedTime = if (status.isCheckedIn) elapsedTime else "",
                onClick = {
                    if (!status.isCheckedIn) {
                        showEngagementSheet = true
                    } else {
                        // Check out
                        status = status.copy(isCheckedIn = false, currentEngagement = null)
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Current engagement
            if (status.isCheckedIn && status.currentEngagement != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CheckedInGreen.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CURRENT ENGAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CheckedInGreen,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        Spacer(Modifier.height(8.dp))
                        EngagementChip(engagement = status.currentEngagement!!, selected = true)
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
                    value = "${status.sessions.size}",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    label = "Engagements",
                    value = "${status.sessions.map { it.engagementId }.distinct().size}",
                    icon = Icons.Default.BusinessCenter,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    label = "This Week",
                    value = "%.0fh".format(status.hoursToday * 3.2f),
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons row
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
                        if (pendingTaskCount > 0) {
                            Badge { Text("$pendingTaskCount") }
                        }
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
            onDismiss = { showEngagementSheet = false },
            onSelect = { engagement ->
                status = status.copy(
                    isCheckedIn = true,
                    currentEngagement = engagement
                )
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp, 14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

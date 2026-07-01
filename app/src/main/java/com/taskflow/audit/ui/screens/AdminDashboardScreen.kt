package com.taskflow.audit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.audit.data.mock.MockData
import com.taskflow.audit.ui.components.AlertsBanner
import com.taskflow.audit.ui.components.StaffStatusRow
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.CheckedOutRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminId: String,
    onNavigateToStaffDetail: (String) -> Unit,
    onNavigateToEngagements: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLogbook: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val statuses = MockData.getStaffStatuses()
    val checkedInCount = statuses.count { it.isCheckedIn }
    val notLoggedAlerts = MockData.notLoggedToday.mapNotNull { id ->
        MockData.staffMembers.find { it.id == id }?.fullName
    }.map { "$it has not logged in today" }

    val today = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(today, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEngagements) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = "Engagements")
                    }
                    IconButton(onClick = onNavigateToTasks) {
                        Icon(Icons.Default.CheckBox, contentDescription = "Tasks")
                    }
                    IconButton(onClick = onNavigateToLogbook) {
                        Icon(Icons.Default.Book, contentDescription = "Logbook")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Alerts banner
            item {
                if (notLoggedAlerts.isNotEmpty()) {
                    AlertsBanner(messages = notLoggedAlerts)
                }
            }

            // Overview cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        label = "Checked In",
                        value = "$checkedInCount/${statuses.size}",
                        icon = Icons.Default.Login,
                        tint = CheckedInGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "Active Clients",
                        value = "${MockData.engagements.size - 1}",
                        icon = Icons.Default.BusinessCenter,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "Not Logged",
                        value = "${MockData.notLoggedToday.size}",
                        icon = Icons.Default.Warning,
                        tint = CheckedOutRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Team section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TEAM STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$checkedInCount active now",
                        style = MaterialTheme.typography.labelSmall,
                        color = CheckedInGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Staff list
            items(statuses) { status ->
                StaffStatusRow(
                    status = status,
                    onClick = { onNavigateToStaffDetail(status.staff.id) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.08f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp, 14.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(22.dp), tint = tint)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tint)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

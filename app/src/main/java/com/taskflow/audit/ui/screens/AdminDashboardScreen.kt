package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.taskflow.audit.data.model.StaffDocument
import com.taskflow.audit.data.model.TimeSessionDocument
import com.taskflow.audit.ui.components.AlertsBanner
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.CheckedOutRed
import com.taskflow.audit.ui.theme.WarningAmber
import com.taskflow.audit.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    vm: AdminViewModel,
    onNavigateToStaffDetail: (String) -> Unit,
    onNavigateToEngagements: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLogbook: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())

    val notLoggedAlerts = state.notLoggedToday.map { "${it.fullName} has not logged in today" }

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
                    IconButton(onClick = onNavigateToEngagements) { Icon(Icons.Default.BusinessCenter, "Engagements") }
                    IconButton(onClick = onNavigateToTasks) { Icon(Icons.Default.CheckBox, "Tasks") }
                    IconButton(onClick = onNavigateToLogbook) { Icon(Icons.Default.Book, "Logbook") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, "Logout") }
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (notLoggedAlerts.isNotEmpty()) {
                item { AlertsBanner(messages = notLoggedAlerts) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        label = "Checked In",
                        value = "${state.checkedInCount}/${state.staffList.size}",
                        icon = Icons.Default.Login,
                        tint = CheckedInGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "Active Clients",
                        value = "${state.engagements.count { it.type != "ADMIN" }}",
                        icon = Icons.Default.BusinessCenter,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "Not Logged",
                        value = "${state.notLoggedToday.size}",
                        icon = Icons.Default.Warning,
                        tint = CheckedOutRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TEAM STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.checkedInCount} active now",
                        style = MaterialTheme.typography.labelSmall,
                        color = CheckedInGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(state.staffList.filter { it.shortId != "admin" }) { staffDoc ->
                val activeSession = state.activeSessions.firstOrNull { it.staffId == staffDoc.uid }
                val engagementName = activeSession?.let { s ->
                    state.engagements.find { it.id == s.engagementId }?.clientName
                }
                FirestoreStaffRow(
                    staff = staffDoc,
                    activeSession = activeSession,
                    engagementName = engagementName,
                    onClick = { onNavigateToStaffDetail(staffDoc.uid) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FirestoreStaffRow(
    staff: StaffDocument,
    activeSession: TimeSessionDocument?,
    engagementName: String?,
    onClick: () -> Unit
) {
    val isCheckedIn = activeSession != null
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(staff.colorHex))
    } catch (_: Exception) { Color(0xFF1565C0) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).background(avatarColor, CircleShape)
            ) {
                Text(staff.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isCheckedIn) CheckedInGreen else WarningAmber,
                        shape = CircleShape
                    )
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(staff.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(staff.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (engagementName != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    engagementName,
                    style = MaterialTheme.typography.labelSmall,
                    color = CheckedInGreen,
                    fontWeight = FontWeight.Medium
                )
            } else if (!isCheckedIn) {
                Spacer(Modifier.height(4.dp))
                Text("Not logged today", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (isCheckedIn) "Active" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
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

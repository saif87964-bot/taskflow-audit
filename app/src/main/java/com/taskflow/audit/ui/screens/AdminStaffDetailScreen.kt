package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.taskflow.audit.data.mock.MockData
import com.taskflow.audit.ui.components.SessionTimelineItem
import com.taskflow.audit.ui.components.WorkloadProgressBar
import com.taskflow.audit.ui.theme.CheckedInGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffDetailScreen(
    staffId: String,
    onBack: () -> Unit
) {
    val status = MockData.getStaffStatus(staffId) ?: return
    val staff = status.staff

    val breakdown = status.sessions
        .groupBy { it.engagementId }
        .mapValues { (_, list) ->
            list.sumOf { s ->
                ((s.endTime ?: System.currentTimeMillis()) - s.startTime).toDouble() / 3_600_000.0
            }.toFloat()
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(staff.avatarColor)
                        ) {
                            Text(staff.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(staff.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(staff.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
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

            // Status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.isCheckedIn)
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
                                if (status.isCheckedIn) "Currently Active" else "Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (status.currentEngagement != null) {
                                Text(
                                    status.currentEngagement.clientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = status.currentEngagement.color
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "%.1fh".format(status.hoursToday),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Workload per engagement
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
                            val eng = MockData.getEngagementById(engId) ?: return@forEach
                            WorkloadProgressBar(
                                label = eng.clientName,
                                hoursUsed = hours,
                                hoursBudget = MockData.weeklyBudgetHours[engId] ?: 8f,
                                color = eng.color
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

            if (status.sessions.isEmpty()) {
                item {
                    Text(
                        "No sessions logged today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                itemsIndexed(status.sessions) { idx, session ->
                    val eng = MockData.getEngagementById(session.engagementId)
                    SessionTimelineItem(
                        session = session,
                        engagement = eng,
                        isLast = idx == status.sessions.lastIndex
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

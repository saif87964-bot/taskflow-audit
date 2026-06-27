package com.taskflow.audit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.audit.data.mock.MockData
import com.taskflow.audit.ui.components.EngagementChip
import com.taskflow.audit.ui.components.SessionTimelineItem
import com.taskflow.audit.ui.theme.CheckedInGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTimesheetScreen(
    staffId: String,
    onBack: () -> Unit
) {
    val status = MockData.getStaffStatus(staffId) ?: return
    val sessions = status.sessions
    val today = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

    // Compute breakdown per engagement
    val breakdown = sessions
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
                    Column {
                        Text("My Timesheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(today, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
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
            // Summary header card
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryItem("Total Today", "%.1fh".format(status.hoursToday), CheckedInGreen)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        SummaryItem("Sessions", "${sessions.size}", MaterialTheme.colorScheme.onPrimaryContainer)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        SummaryItem("Clients", "${breakdown.size}", MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Engagement breakdown
            item {
                Text(
                    "TIME BY ENGAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                breakdown.forEach { (engId, hours) ->
                    val eng = MockData.getEngagementById(engId) ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EngagementChip(engagement = eng, compact = true)
                        Text(
                            "%.1fh".format(hours),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = eng.color
                        )
                    }
                }
            }

            // Timeline header
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "SESSION TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            if (sessions.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    ) {
                        Text(
                            "No sessions logged today.\nCheck in to start tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(sessions) { index, session ->
                    val engagement = MockData.getEngagementById(session.engagementId)
                    SessionTimelineItem(
                        session = session,
                        engagement = engagement,
                        isLast = index == sessions.lastIndex
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

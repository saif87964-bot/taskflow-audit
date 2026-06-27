package com.taskflow.audit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.audit.data.mock.Engagement
import com.taskflow.audit.data.mock.EngagementType
import com.taskflow.audit.data.mock.MockData
import com.taskflow.audit.ui.components.WorkloadProgressBar
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.NonBillableGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEngagementsScreen(onBack: () -> Unit) {
    val engagements = MockData.engagements
    val billable = engagements.filter { it.type != EngagementType.ADMIN }
    val internal = engagements.filter { it.type == EngagementType.ADMIN }

    val totalBillableHours = MockData.weeklyEngagementHours
        .filterKeys { id -> engagements.find { it.id == id }?.type != EngagementType.ADMIN } != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Engagements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add engagement")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryCol("Engagements", "${billable.size}")
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        val totalHours = MockData.weeklyEngagementHours.values.sum()
                        SummaryCol("Total Hrs (wk)", "%.0fh".format(totalHours))
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        val totalBudget = MockData.weeklyBudgetHours.values.sum()
                        SummaryCol("Budget Hrs (wk)", "%.0fh".format(totalBudget))
                    }
                }
            }

            // Client engagements
            item {
                Text(
                    "CLIENT ENGAGEMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(billable) { eng ->
                EngagementCard(engagement = eng)
            }

            // Internal
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "INTERNAL / ADMIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(internal) { eng ->
                EngagementCard(engagement = eng)
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EngagementCard(engagement: Engagement) {
    val hours = MockData.weeklyEngagementHours[engagement.id] ?: 0f
    val budget = MockData.weeklyBudgetHours[engagement.id] ?: 0f
    val staffOnEng = MockData.getStaffStatuses()
        .filter { it.currentEngagement?.id == engagement.id }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        engagement.clientName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        engagement.code + " • " + engagement.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = engagement.color
                    )
                }
                if (staffOnEng.isNotEmpty()) {
                    SurfaceBadge(
                        text = "${staffOnEng.size} active",
                        color = CheckedInGreen
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            WorkloadProgressBar(
                label = "This week",
                hoursUsed = hours,
                hoursBudget = budget,
                color = if (engagement.type == EngagementType.ADMIN) NonBillableGray else engagement.color
            )
        }
    }
}

@Composable
private fun SurfaceBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SummaryCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

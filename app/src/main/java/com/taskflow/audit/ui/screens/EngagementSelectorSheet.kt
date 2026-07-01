package com.taskflow.audit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.ui.components.EngagementChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementSelectorSheet(
    engagements: List<EngagementDocument>,
    onDismiss: () -> Unit,
    onSelect: (engagementId: String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = engagements.filter {
        query.isEmpty() ||
        it.clientName.contains(query, ignoreCase = true) ||
        it.code.contains(query, ignoreCase = true)
    }
    val billable = filtered.filter { it.type != "ADMIN" }
    val internal = filtered.filter { it.type == "ADMIN" }
    val recent = filtered.take(3)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Select Engagement",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Switching will automatically close your current session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search client or code…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (engagements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 480.dp)
                ) {
                    if (query.isEmpty()) {
                        item {
                            Text("RECENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                        }
                        items(recent) { eng ->
                            EngagementDocListItem(engagement = eng, onClick = { onSelect(eng.id) })
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    if (billable.isNotEmpty()) {
                        item {
                            Text("CLIENT ENGAGEMENTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                        }
                        items(billable) { eng ->
                            EngagementDocListItem(engagement = eng, onClick = { onSelect(eng.id) })
                        }
                    }

                    if (internal.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("INTERNAL / ADMIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                        }
                        items(internal) { eng ->
                            EngagementDocListItem(engagement = eng, onClick = { onSelect(eng.id) })
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EngagementDocListItem(engagement: EngagementDocument, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(engagement.clientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(engagement.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(engagement.type, style = MaterialTheme.typography.labelSmall)
            }
        },
        leadingContent = {
            EngagementChip(doc = engagement, compact = true)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        tonalElevation = 0.dp
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
}

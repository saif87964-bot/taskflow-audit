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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.audit.data.model.TaskDocument
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.CheckedOutRed
import com.taskflow.audit.ui.theme.WarningAmber
import com.taskflow.audit.ui.viewmodel.TasksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    vm: TasksViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filterStatus by remember { mutableStateOf<String?>(null) }

    val displayed = if (filterStatus == null) state.tasks
                    else state.tasks.filter { it.status == filterStatus }
    val pendingCount = state.tasks.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isAdmin) "All Tasks" else "My Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$pendingCount pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pendingCount > 0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("All (${state.tasks.size})") })
                    FilterChip(selected = filterStatus == "PENDING", onClick = { filterStatus = "PENDING" }, label = { Text("Pending") })
                    FilterChip(selected = filterStatus == "IN_PROGRESS", onClick = { filterStatus = "IN_PROGRESS" }, label = { Text("Active") })
                    FilterChip(selected = filterStatus == "DONE", onClick = { filterStatus = "DONE" }, label = { Text("Done") })
                }
            }

            if (displayed.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = CheckedInGreen)
                            Spacer(Modifier.height(12.dp))
                            Text("All clear!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("No tasks in this category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(displayed, key = { it.id }) { task ->
                TaskDocCard(
                    task = task,
                    showAssignee = isAdmin,
                    onToggle = { newStatus -> vm.updateStatus(task.id, newStatus) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TaskDocCard(
    task: TaskDocument,
    showAssignee: Boolean,
    onToggle: (String) -> Unit
) {
    val (priorityColor, priorityLabel) = when (task.priority) {
        "HIGH"   -> CheckedOutRed to "HIGH"
        "MEDIUM" -> WarningAmber to "MEDIUM"
        else     -> Color(0xFF94A3B8) to "LOW"
    }
    val isDone = task.status == "DONE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { checked ->
                        onToggle(if (checked) "DONE" else "PENDING")
                    },
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    colors = CheckboxDefaults.colors(checkedColor = CheckedInGreen)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDone) 0.4f else 1f),
                            maxLines = 2
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text(priorityLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = priorityColor) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = priorityColor.copy(alpha = 0.1f)),
                    border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = priorityColor.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.engagementId.isNotEmpty()) {
                        Text(
                            task.engagementId.take(6).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (showAssignee && task.assigneeId.isNotEmpty()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(task.assigneeId.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
                if (task.dueDate.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            task.dueDate.removePrefix("2026-"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.dueDate <= "2026-07-01" && !isDone) CheckedOutRed
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

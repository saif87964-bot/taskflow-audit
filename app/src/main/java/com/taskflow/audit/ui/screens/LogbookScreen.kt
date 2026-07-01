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
import com.taskflow.audit.data.mock.*
import com.taskflow.audit.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    staffId: String,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    var entries by remember {
        mutableStateOf(
            if (isAdmin) MockData.logEntries else MockData.getLogEntriesForStaff(staffId)
        )
    }
    var filterCategory by remember { mutableStateOf<LogCategory?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val displayed = if (filterCategory == null) entries else entries.filter { it.category == filterCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isAdmin) "Firm Logbook" else "My Logbook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${displayed.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (!isAdmin) {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, "Add entry")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isAdmin) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Edit, "Add entry")
                }
            }
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

            // Category filter chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filterCategory == null,
                        onClick = { filterCategory = null },
                        label = { Text("All") }
                    )
                    LogCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat },
                            label = { Text(cat.name.take(4).lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            if (displayed.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Book, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("No entries yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tap + to log a fieldwork note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(displayed, key = { it.id }) { entry ->
                LogEntryCard(entry = entry, showAuthor = isAdmin)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddSheet) {
        AddLogEntrySheet(
            staffId = staffId,
            onDismiss = { showAddSheet = false },
            onSave = { note, category, engId ->
                val newEntry = LogEntry(
                    id = "l${entries.size + 10}",
                    staffId = staffId,
                    engagementId = engId,
                    note = note,
                    category = category,
                    timestamp = "Just now"
                )
                entries = listOf(newEntry) + entries
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry, showAuthor: Boolean) {
    val engagement = MockData.getEngagementById(entry.engagementId)
    val staff = MockData.staffMembers.find { it.id == entry.staffId }

    val (categoryColor, categoryIcon) = when (entry.category) {
        LogCategory.FINDING -> Color(0xFFEF4444) to Icons.Default.Flag
        LogCategory.OBSERVATION -> Color(0xFF00897B) to Icons.Default.Visibility
        LogCategory.MEETING -> Color(0xFF3B82F6) to Icons.Default.People
        LogCategory.ADMIN -> Color(0xFF94A3B8) to Icons.Default.Description
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    categoryIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = categoryColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    entry.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
                Spacer(Modifier.weight(1f))
                Text(
                    entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                entry.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (engagement != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Circle, null, modifier = Modifier.size(8.dp), tint = engagement.color)
                        Spacer(Modifier.width(4.dp))
                        Text(engagement.code, style = MaterialTheme.typography.labelSmall, color = engagement.color, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text("· ${engagement.clientName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                if (showAuthor && staff != null) {
                    Text(
                        staff.initials,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLogEntrySheet(
    staffId: String,
    onDismiss: () -> Unit,
    onSave: (note: String, category: LogCategory, engagementId: String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(LogCategory.OBSERVATION) }
    var selectedEngId by remember { mutableStateOf(MockData.engagements.first().id) }
    var showEngMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("New Log Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // Category picker
            Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LogCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Engagement picker
            Text("Engagement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            val selectedEng = MockData.getEngagementById(selectedEngId)
            ExposedDropdownMenuBox(expanded = showEngMenu, onExpandedChange = { showEngMenu = it }) {
                OutlinedTextField(
                    value = selectedEng?.clientName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEngMenu) }
                )
                ExposedDropdownMenu(expanded = showEngMenu, onDismissRequest = { showEngMenu = false }) {
                    MockData.engagements.forEach { eng ->
                        DropdownMenuItem(
                            text = { Text(eng.clientName, style = MaterialTheme.typography.bodySmall) },
                            onClick = { selectedEngId = eng.id; showEngMenu = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Note input
            Text("Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Describe your observation, finding, or note…", style = MaterialTheme.typography.bodySmall) },
                maxLines = 5
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (note.isNotBlank()) onSave(note, selectedCategory, selectedEngId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = note.isNotBlank()
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Entry")
            }
        }
    }
}

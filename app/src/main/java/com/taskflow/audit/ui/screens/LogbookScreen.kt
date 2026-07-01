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
import com.taskflow.audit.data.model.LogEntryDocument
import com.taskflow.audit.ui.viewmodel.LogbookViewModel
import java.text.SimpleDateFormat
import java.util.*

private val CATEGORIES = listOf("OBSERVATION", "FINDING", "MEETING", "ADMIN")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    vm: LogbookViewModel,
    uid: String,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val displayed = if (filterCategory == null) state.entries
                    else state.entries.filter { it.category == filterCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isAdmin) "Firm Logbook" else "My Logbook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${displayed.size} entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (!isAdmin) {
                        IconButton(onClick = { showAddSheet = true }) { Icon(Icons.Default.Add, "Add entry") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isAdmin) {
                FloatingActionButton(onClick = { showAddSheet = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Edit, "Add entry")
                }
            }
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filterCategory == null, onClick = { filterCategory = null }, label = { Text("All") })
                    CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat },
                            label = { Text(cat.take(4).lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            if (displayed.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
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
                LogDocCard(entry = entry, showAuthor = isAdmin)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddSheet) {
        AddLogEntryDocSheet(
            engagements = state.engagements.map { it.id to it.clientName },
            onDismiss = { showAddSheet = false },
            onSave = { note, category, engId ->
                vm.addEntry(note, category, engId)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun LogDocCard(entry: LogEntryDocument, showAuthor: Boolean) {
    val (categoryColor, categoryIcon) = when (entry.category) {
        "FINDING"     -> Color(0xFFEF4444) to Icons.Default.Flag
        "OBSERVATION" -> Color(0xFF00897B) to Icons.Default.Visibility
        "MEETING"     -> Color(0xFF3B82F6) to Icons.Default.People
        else          -> Color(0xFF94A3B8) to Icons.Default.Description
    }

    val timestamp = entry.createdAt?.toDate()?.let {
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(it)
    } ?: "Just now"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(categoryIcon, null, modifier = Modifier.size(16.dp), tint = categoryColor)
                Spacer(Modifier.width(6.dp))
                Text(entry.category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = categoryColor)
                Spacer(Modifier.weight(1f))
                Text(timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            if (showAuthor && entry.staffId.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Text(
                    entry.staffId.take(5).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLogEntryDocSheet(
    engagements: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (note: String, category: String, engagementId: String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("OBSERVATION") }
    var selectedEngId by remember { mutableStateOf(engagements.firstOrNull()?.first ?: "") }
    var showEngMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("New Log Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Engagement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            val selectedName = engagements.find { it.first == selectedEngId }?.second ?: ""
            ExposedDropdownMenuBox(expanded = showEngMenu, onExpandedChange = { showEngMenu = it }) {
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEngMenu) }
                )
                ExposedDropdownMenu(expanded = showEngMenu, onDismissRequest = { showEngMenu = false }) {
                    engagements.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodySmall) },
                            onClick = { selectedEngId = id; showEngMenu = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

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
                onClick = { if (note.isNotBlank() && selectedEngId.isNotEmpty()) onSave(note, selectedCategory, selectedEngId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = note.isNotBlank() && selectedEngId.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Entry")
            }
        }
    }
}

package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.taskflow.audit.data.model.TaskDocument
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.CheckedOutRed
import com.taskflow.audit.ui.theme.WarningAmber
import com.taskflow.audit.ui.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    vm: TasksViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val displayed = if (filterStatus == null) state.tasks
                    else state.tasks.filter { it.status == filterStatus }
    val pendingCount = state.tasks.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LaunchedEffect(state.createSuccess) {
        if (state.createSuccess) {
            showAddSheet = false
            snackbarHostState.showSnackbar("Task assigned")
            vm.clearMessages()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessages()
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, sheetState = sheetState) {
            AddTaskSheet(
                staff = state.staff.map { it.uid to it.fullName },
                engagements = state.engagements.map { it.id to it.clientName },
                onDismiss = { showAddSheet = false },
                onConfirm = { title, desc, assignee, engagement, priority, due ->
                    vm.createTask(title, desc, assignee, engagement, priority, due)
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            "$pendingCount open",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pendingCount > 0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Task") }
                )
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("All (${state.tasks.size})") }) }
                    item { FilterChip(selected = filterStatus == "PENDING", onClick = { filterStatus = "PENDING" }, label = { Text("Pending") }) }
                    item { FilterChip(selected = filterStatus == "IN_PROGRESS", onClick = { filterStatus = "IN_PROGRESS" }, label = { Text("Active") }) }
                    item { FilterChip(selected = filterStatus == "DONE", onClick = { filterStatus = "DONE" }, label = { Text("Done") }) }
                }
            }

            if (displayed.isEmpty()) {
                item {
                    EmptyState(
                        icon = if (state.tasks.isEmpty()) Icons.Default.AssignmentTurnedIn else Icons.Default.CheckCircle,
                        title = if (state.tasks.isEmpty()) "No tasks yet" else "All clear!",
                        subtitle = when {
                            state.tasks.isEmpty() && isAdmin -> "Tap “New Task” to assign work to your team"
                            state.tasks.isEmpty() -> "Tasks assigned to you will appear here"
                            else -> "Nothing in this category"
                        }
                    )
                }
            }

            items(displayed, key = { it.id }) { task ->
                TaskDocCard(
                    task = task,
                    assigneeName = state.staffByUid(task.assigneeId)?.let { s -> s.initials.ifBlank { s.fullName.take(2).uppercase() } },
                    engagementCode = state.engagementById(task.engagementId)?.code,
                    todayStr = todayStr,
                    isAdmin = isAdmin,
                    onToggle = { newStatus -> vm.updateStatus(task.id, newStatus) },
                    onDelete = { vm.deleteTask(task.id) }
                )
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            ) {
                Icon(icon, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TaskDocCard(
    task: TaskDocument,
    assigneeName: String?,
    engagementCode: String?,
    todayStr: String,
    isAdmin: Boolean,
    onToggle: (String) -> Unit,
    onDelete: () -> Unit
) {
    val (priorityColor, priorityLabel) = when (task.priority) {
        "HIGH"   -> CheckedOutRed to "HIGH"
        "MEDIUM" -> WarningAmber to "MEDIUM"
        else     -> Color(0xFF94A3B8) to "LOW"
    }
    val isDone = task.status == "DONE"
    val isOverdue = task.dueDate.isNotEmpty() && task.dueDate < todayStr && !isDone
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { checked ->
                        onToggle(if (checked) "DONE" else "PENDING")
                    },
                    colors = CheckboxDefaults.colors(checkedColor = CheckedInGreen)
                )
                Spacer(Modifier.width(4.dp))
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
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = priorityColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            priorityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isAdmin) {
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.MoreVert, "Task options",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete task") },
                                    onClick = { showMenu = false; onDelete() },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (engagementCode != null) {
                        Text(
                            engagementCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (assigneeName != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Text(
                                assigneeName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (task.dueDate.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday, null,
                            modifier = Modifier.size(11.dp),
                            tint = if (isOverdue) CheckedOutRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (isOverdue) "${task.dueDate} · overdue" else task.dueDate,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) CheckedOutRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
    staff: List<Pair<String, String>>,
    engagements: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, assigneeId: String, engagementId: String, priority: String, dueDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assigneeId by remember { mutableStateOf(staff.firstOrNull()?.first ?: "") }
    var engagementId by remember { mutableStateOf(engagements.firstOrNull()?.first ?: "") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var dueDate by remember { mutableStateOf("") }
    var assigneeMenu by remember { mutableStateOf(false) }
    var engagementMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dateState) }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Text("New Task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Details (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Spacer(Modifier.height(10.dp))

        val assigneeName = staff.find { it.first == assigneeId }?.second ?: ""
        ExposedDropdownMenuBox(expanded = assigneeMenu, onExpandedChange = { assigneeMenu = it }) {
            OutlinedTextField(
                value = assigneeName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Assign to") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = assigneeMenu, onDismissRequest = { assigneeMenu = false }) {
                staff.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { assigneeId = id; assigneeMenu = false })
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        val engagementName = engagements.find { it.first == engagementId }?.second ?: ""
        ExposedDropdownMenuBox(expanded = engagementMenu, onExpandedChange = { engagementMenu = it }) {
            OutlinedTextField(
                value = engagementName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Engagement") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engagementMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = engagementMenu, onDismissRequest = { engagementMenu = false }) {
                engagements.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { engagementId = id; engagementMenu = false })
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Text("Priority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { priority = p },
                    label = { Text(p.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = dueDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Due date (optional)") },
            placeholder = { Text("Tap the calendar to pick") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, "Pick due date")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onConfirm(title, description, assigneeId, engagementId, priority, dueDate) },
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank() && assigneeId.isNotEmpty() && engagementId.isNotEmpty()
            ) { Text("Assign Task") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.ui.viewmodel.AdminEngagementViewModel

private val colorPresets = listOf(
    "#1565C0", "#00695C", "#6A1B9A", "#E65100",
    "#37474F", "#C62828", "#00838F", "#F57F17"
)

private val engagementTypes = listOf("AUDIT", "TAX", "ADVISORY", "ADMIN")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEngagementsScreen(
    vm: AdminEngagementViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedEngagement by remember { mutableStateOf<EngagementDocument?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessages()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessages()
        }
    }

    val billable = state.engagements.filter { it.type != "ADMIN" }
    val internal = state.engagements.filter { it.type == "ADMIN" }
    val totalBudgetHours = state.engagements.sumOf { it.budgetHours }

    // Add sheet
    if (showAddSheet) {
        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState
        ) {
            AddEngagementSheet(
                onDismiss = { showAddSheet = false },
                onConfirm = { code, clientName, type, colorHex, budgetHours ->
                    vm.createEngagement(code, clientName, type, colorHex, budgetHours)
                    showAddSheet = false
                }
            )
        }
    }

    // Options sheet (edit/archive)
    if (showOptionsSheet && selectedEngagement != null) {
        val optSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = optSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        showOptionsSheet = false
                        showEditSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Engagement")
                }
                TextButton(
                    onClick = {
                        showOptionsSheet = false
                        showArchiveConfirm = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Archive Engagement", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Edit sheet
    if (showEditSheet && selectedEngagement != null) {
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = editSheetState
        ) {
            EditEngagementSheet(
                engagement = selectedEngagement!!,
                onDismiss = { showEditSheet = false },
                onUpdate = { code, clientName, type, colorHex, budgetHours ->
                    vm.updateEngagement(selectedEngagement!!.id, code, clientName, type, colorHex, budgetHours)
                    showEditSheet = false
                },
                onArchive = {
                    showEditSheet = false
                    showArchiveConfirm = true
                }
            )
        }
    }

    // Archive confirm dialog
    if (showArchiveConfirm && selectedEngagement != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive Engagement?") },
            text = { Text("${selectedEngagement!!.clientName} will be marked inactive and hidden from active lists.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.archiveEngagement(selectedEngagement!!.id)
                        showArchiveConfirm = false
                        selectedEngagement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Engagements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add engagement")
                    }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Summary card
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
                        EngSummaryCol("Billable", "${billable.size}")
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        EngSummaryCol("Budget Hrs", "${totalBudgetHours}h")
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
                EngagementFirestoreCard(
                    engagement = eng,
                    onClick = {
                        selectedEngagement = eng
                        showOptionsSheet = true
                    }
                )
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
                EngagementFirestoreCard(
                    engagement = eng,
                    onClick = {
                        selectedEngagement = eng
                        showOptionsSheet = true
                    }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EngagementFirestoreCard(engagement: EngagementDocument, onClick: () -> Unit) {
    val dotColor = try {
        Color(android.graphics.Color.parseColor(engagement.colorHex))
    } catch (_: Exception) { Color(0xFF00897B) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    engagement.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        engagement.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = dotColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            engagement.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = dotColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                "${engagement.budgetHours}h",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EngSummaryCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun AddEngagementSheet(
    onDismiss: () -> Unit,
    onConfirm: (code: String, clientName: String, type: String, colorHex: String, budgetHours: Int) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("AUDIT") }
    var budgetHoursText by remember { mutableStateOf("20") }
    var selectedColor by remember { mutableStateOf(colorPresets[0]) }

    // Auto-suggest code from clientName initials
    LaunchedEffect(clientName) {
        if (code.isEmpty() || code == clientName.take(clientName.length - 1)
                .split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")) {
            code = clientName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").take(4)
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Text("Add Engagement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = clientName,
            onValueChange = { clientName = it },
            label = { Text("Client Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(6) },
            label = { Text("Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(engagementTypes) { t ->
                FilterChip(
                    selected = selectedType == t,
                    onClick = { selectedType = t },
                    label = { Text(t) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = budgetHoursText,
            onValueChange = { budgetHoursText = it.filter { c -> c.isDigit() } },
            label = { Text("Budget Hours") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorPresets.forEach { hex ->
                val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                Box(
                    modifier = Modifier
                        .size(if (selectedColor == hex) 34.dp else 28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .clickable { selectedColor = hex }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    val hours = budgetHoursText.toIntOrNull() ?: 20
                    onConfirm(code, clientName, selectedType, selectedColor, hours)
                },
                modifier = Modifier.weight(1f),
                enabled = clientName.isNotBlank() && code.isNotBlank()
            ) { Text("Create") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EditEngagementSheet(
    engagement: EngagementDocument,
    onDismiss: () -> Unit,
    onUpdate: (code: String, clientName: String, type: String, colorHex: String, budgetHours: Int) -> Unit,
    onArchive: () -> Unit
) {
    var clientName by remember { mutableStateOf(engagement.clientName) }
    var code by remember { mutableStateOf(engagement.code) }
    var selectedType by remember { mutableStateOf(engagement.type) }
    var budgetHoursText by remember { mutableStateOf(engagement.budgetHours.toString()) }
    var selectedColor by remember { mutableStateOf(engagement.colorHex) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Text("Edit Engagement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = clientName,
            onValueChange = { clientName = it },
            label = { Text("Client Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(6) },
            label = { Text("Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(engagementTypes) { t ->
                FilterChip(
                    selected = selectedType == t,
                    onClick = { selectedType = t },
                    label = { Text(t) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = budgetHoursText,
            onValueChange = { budgetHoursText = it.filter { c -> c.isDigit() } },
            label = { Text("Budget Hours") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorPresets.forEach { hex ->
                val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                Box(
                    modifier = Modifier
                        .size(if (selectedColor == hex) 34.dp else 28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .clickable { selectedColor = hex }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    val hours = budgetHoursText.toIntOrNull() ?: 20
                    onUpdate(code, clientName, selectedType, selectedColor, hours)
                },
                modifier = Modifier.weight(1f),
                enabled = clientName.isNotBlank() && code.isNotBlank()
            ) { Text("Save") }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onArchive,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Archive Engagement") }
        Spacer(Modifier.height(16.dp))
    }
}

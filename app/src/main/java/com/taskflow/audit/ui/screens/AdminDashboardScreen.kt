package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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

private val dashboardColorPresets = listOf(
    "#1565C0", "#00695C", "#6A1B9A", "#E65100",
    "#37474F", "#C62828", "#00838F", "#F57F17"
)

private val staffRoleOptions = listOf(
    "Senior Auditor", "Audit Manager", "Auditor",
    "Tax Consultant", "Partner", "Associate", "Administrator"
)

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
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddStaffSheet by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffDocument?>(null) }

    val notLoggedAlerts = state.notLoggedToday.map { "${it.fullName} has not logged in today" }

    LaunchedEffect(state.createStaffSuccess) {
        if (state.createStaffSuccess) {
            showAddStaffSheet = false
            snackbarHostState.showSnackbar("Staff account created. Default PIN: 1234")
            vm.clearActionResult()
        }
    }
    LaunchedEffect(state.updateStaffSuccess) {
        if (state.updateStaffSuccess) {
            editingStaff = null
            snackbarHostState.showSnackbar("Staff updated successfully")
            vm.clearActionResult()
        }
    }
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionResult()
        }
    }
    LaunchedEffect(state.resetPinSuccess) {
        state.resetPinSuccess?.let {
            snackbarHostState.showSnackbar("$it will be asked to set a new PIN at next login")
            vm.clearActionResult()
        }
    }

    if (showAddStaffSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddStaffSheet = false },
            sheetState = sheetState
        ) {
            AddStaffSheet(
                onDismiss = { showAddStaffSheet = false },
                onConfirm = { shortId, fullName, role, colorHex, isAdmin ->
                    vm.createStaff(shortId, fullName, role, colorHex, isAdmin)
                }
            )
        }
    }

    editingStaff?.let { staff ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingStaff = null },
            sheetState = sheetState
        ) {
            EditStaffSheet(
                staff = staff,
                onDismiss = { editingStaff = null },
                onConfirm = { fullName, role, colorHex, isAdmin ->
                    vm.updateStaff(staff.uid, fullName, role, colorHex, isAdmin)
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStaffSheet = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff")
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
                    hasSessionToday = state.todaySessions.any { it.staffId == staffDoc.uid },
                    engagementName = engagementName,
                    onClick = { onNavigateToStaffDetail(staffDoc.uid) },
                    onEdit = { editingStaff = staffDoc },
                    onResetPin = { vm.resetStaffPin(staffDoc.uid) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FirestoreStaffRow(
    staff: StaffDocument,
    activeSession: TimeSessionDocument?,
    hasSessionToday: Boolean,
    engagementName: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onResetPin: () -> Unit
) {
    val isCheckedIn = activeSession != null
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(staff.colorHex))
    } catch (_: Exception) { Color(0xFF1565C0) }
    var showMenu by remember { mutableStateOf(false) }

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
                if (hasSessionToday) {
                    Text("Checked out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Not logged today", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (isCheckedIn) "Active" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Reset PIN") },
                        onClick = { showMenu = false; onResetPin() },
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStaffSheet(
    staff: StaffDocument,
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, role: String, colorHex: String, isAdmin: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf(staff.fullName) }
    var selectedRole by remember { mutableStateOf(staff.role.ifBlank { staffRoleOptions[0] }) }
    var isAdmin by remember { mutableStateOf(staff.isAdmin) }
    var selectedColor by remember { mutableStateOf(staff.colorHex.ifBlank { dashboardColorPresets[0] }) }
    var roleExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Text("Edit Staff Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(staff.shortId.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = !roleExpanded }) {
            OutlinedTextField(
                value = selectedRole,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                staffRoleOptions.forEach { role ->
                    DropdownMenuItem(text = { Text(role) }, onClick = { selectedRole = role; roleExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Admin Access", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Can access admin dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isAdmin, onCheckedChange = { isAdmin = it })
        }
        Spacer(Modifier.height(10.dp))

        Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(dashboardColorPresets) { hex ->
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
                onClick = { onConfirm(fullName, selectedRole, selectedColor, isAdmin) },
                modifier = Modifier.weight(1f),
                enabled = fullName.isNotBlank()
            ) { Text("Save Changes") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffSheet(
    onDismiss: () -> Unit,
    onConfirm: (shortId: String, fullName: String, role: String, colorHex: String, isAdmin: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var shortId by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(staffRoleOptions[0]) }
    var isAdmin by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(dashboardColorPresets[0]) }
    var roleExpanded by remember { mutableStateOf(false) }

    // Auto-suggest shortId from initials — but stop once the admin edits it manually
    var shortIdEdited by remember { mutableStateOf(false) }
    LaunchedEffect(fullName) {
        if (!shortIdEdited) {
            shortId = fullName.trim().split(" ")
                .mapNotNull { it.firstOrNull()?.lowercaseChar() }
                .take(2)
                .joinToString("")
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Text("Add Staff Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = shortId,
            onValueChange = { if (it.length <= 3) { shortId = it.lowercase(); shortIdEdited = true } },
            label = { Text("Short ID (max 3 chars)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Used as login ID, e.g. 'im' for Imtiaz M") }
        )
        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(
            expanded = roleExpanded,
            onExpandedChange = { roleExpanded = !roleExpanded }
        ) {
            OutlinedTextField(
                value = selectedRole,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false }
            ) {
                staffRoleOptions.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            selectedRole = role
                            roleExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Admin Access", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Can access admin dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isAdmin, onCheckedChange = { isAdmin = it })
        }
        Spacer(Modifier.height(10.dp))

        Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(dashboardColorPresets) { hex ->
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
                onClick = { onConfirm(shortId, fullName, selectedRole, selectedColor, isAdmin) },
                modifier = Modifier.weight(1f),
                enabled = fullName.isNotBlank() && shortId.isNotBlank()
            ) { Text("Create Staff") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

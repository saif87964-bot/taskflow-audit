package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.audit.data.mock.MockData
import com.taskflow.audit.ui.theme.Navy900

@Composable
fun LoginScreen(onLogin: (staffId: String, isAdmin: Boolean) -> Unit) {
    var selectedStaffId by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val selectedStaff = MockData.staffMembers.find { it.id == selectedStaffId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Navy900, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Logo / Brand
            Text(
                text = "TaskFlow",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "AUDIT",
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Time Tracking & Engagement Management",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Staff selector
            Text(
                text = "SELECT STAFF",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MockData.staffMembers.forEach { staff ->
                    val isSelected = staff.id == selectedStaffId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedStaffId = staff.id
                                pin = ""
                                showError = false
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) staff.avatarColor else staff.avatarColor.copy(alpha = 0.4f))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = staff.initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = staff.initials,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            if (selectedStaff != null) {
                Text(
                    text = selectedStaff.fullName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = selectedStaff.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(24.dp))

                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < pin.length) MaterialTheme.colorScheme.tertiary
                                    else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                if (showError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Incorrect PIN. Try 1234.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(Modifier.height(28.dp))

                // PIN pad
                PinPad(
                    onDigit = {
                        if (pin.length < 4) {
                            pin += it
                            showError = false
                            if (pin.length == 4) {
                                if (pin == "1234") {
                                    onLogin(selectedStaffId, selectedStaff.isAdmin)
                                } else {
                                    showError = true
                                    pin = ""
                                }
                            }
                        }
                    },
                    onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                )
            } else {
                Text(
                    text = "Select a staff member above to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (key.isEmpty()) Color.Transparent
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .then(
                                if (key.isNotEmpty()) Modifier.clickable {
                                    if (key == "DEL") onDelete() else onDigit(key)
                                } else Modifier
                            )
                    ) {
                        if (key == "DEL") {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

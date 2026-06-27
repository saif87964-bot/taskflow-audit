package com.taskflow.audit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.audit.data.mock.StaffStatus
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.CheckedOutRed
import com.taskflow.audit.ui.theme.WarningAmber

@Composable
fun StaffStatusRow(
    status: StaffStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        // Avatar with online dot
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(status.staff.avatarColor, CircleShape)
            ) {
                Text(
                    text = status.staff.initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            // Status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when {
                            status.isCheckedIn -> CheckedInGreen
                            status.hoursToday == 0f -> WarningAmber
                            else -> CheckedOutRed
                        },
                        shape = CircleShape
                    )
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.staff.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status.staff.role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (status.currentEngagement != null) {
                Spacer(Modifier.height(4.dp))
                EngagementChip(
                    engagement = status.currentEngagement,
                    selected = false,
                    compact = true
                )
            } else if (!status.isCheckedIn && status.hoursToday == 0f) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Not logged today",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "%.1fh".format(status.hoursToday),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (status.isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (status.isCheckedIn) "Active" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = if (status.isCheckedIn) CheckedInGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
import com.taskflow.audit.data.mock.Engagement
import com.taskflow.audit.data.mock.EngagementType

@Composable
fun EngagementChip(
    engagement: Engagement,
    selected: Boolean = false,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) engagement.color else engagement.color.copy(alpha = 0.12f)
    val textColor = if (selected) Color.White else engagement.color
    val shape = RoundedCornerShape(50)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .then(
                if (!selected) Modifier.border(1.dp, engagement.color.copy(alpha = 0.3f), shape)
                else Modifier
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        // Avatar circle with initials
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (compact) 18.dp else 22.dp)
                .background(
                    if (selected) Color.White.copy(alpha = 0.25f) else engagement.color,
                    CircleShape
                )
        ) {
            Text(
                text = engagement.code.take(2),
                color = Color.White,
                fontSize = if (compact) 7.sp else 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = if (compact && engagement.clientName.length > 18)
                engagement.clientName.take(16) + "…"
            else engagement.clientName,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )

        if (engagement.type == EngagementType.ADMIN) {
            Text(
                text = "INTERNAL",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

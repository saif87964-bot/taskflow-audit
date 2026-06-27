package com.taskflow.audit.ui.components

import androidx.compose.foundation.background
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
import com.taskflow.audit.data.mock.Engagement
import com.taskflow.audit.data.mock.TimeSession
import com.taskflow.audit.ui.theme.CheckedInGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionTimelineItem(
    session: TimeSession,
    engagement: Engagement?,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val startFormatted = formatTime(session.startTime)
    val endFormatted = session.endTime?.let { formatTime(it) } ?: "Now"
    val durationMinutes = ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 60_000
    val durationText = "%dh %02dm".format(durationMinutes / 60, durationMinutes % 60)
    val engColor = engagement?.color ?: Color.Gray

    Row(modifier = modifier.fillMaxWidth()) {
        // Timeline line + dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (session.endTime == null) CheckedInGreen else engColor,
                        CircleShape
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Session card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(10.dp, 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = engagement?.clientName ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = engColor
                )
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (session.endTime == null) CheckedInGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$startFormatted – $endFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

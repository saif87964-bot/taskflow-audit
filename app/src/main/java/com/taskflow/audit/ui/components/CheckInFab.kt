package com.taskflow.audit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.audit.ui.theme.CheckedInGreen
import com.taskflow.audit.ui.theme.CheckedOutRed

@Composable
fun CheckInFab(
    isCheckedIn: Boolean,
    elapsedTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fabColor by animateColorAsState(
        targetValue = if (isCheckedIn) CheckedInGreen else CheckedOutRed,
        animationSpec = tween(durationMillis = 300),
        label = "fab_color"
    )

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isCheckedIn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outer pulse ring
            if (isCheckedIn) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .background(CheckedInGreen.copy(alpha = 0.18f), CircleShape)
                )
            }
            // Main FAB
            FloatingActionButton(
                onClick = onClick,
                containerColor = fabColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isCheckedIn) Icons.Default.Logout else Icons.Default.Login,
                    contentDescription = if (isCheckedIn) "Check Out" else "Check In",
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isCheckedIn) "CHECK OUT" else "CHECK IN",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = fabColor
        )

        if (isCheckedIn) {
            Text(
                text = elapsedTime,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

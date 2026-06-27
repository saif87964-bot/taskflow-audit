package com.taskflow.audit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Navy900,
    onPrimary = Color.White,
    primaryContainer = Navy50,
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Teal700,
    onSecondary = Color.White,
    secondaryContainer = Teal50,
    onSecondaryContainer = Color(0xFF003733),
    tertiary = Green500,
    onTertiary = Color.White,
    tertiaryContainer = Green50,
    onTertiaryContainer = Color(0xFF00391B),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate600,
    error = Red600,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Color(0xFF7F1D1D),
    outline = SlateLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF001D36),
    primaryContainer = Navy900,
    onPrimaryContainer = Navy50,
    secondary = TealLight,
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Teal50,
    tertiary = GreenBright,
    onTertiary = Color(0xFF00391B),
    tertiaryContainer = Color(0xFF00692F),
    onTertiaryContainer = Green50,
    background = OledBlack,
    onBackground = Slate50,
    surface = DarkSurface,
    onSurface = Slate50,
    surfaceVariant = DarkVariant,
    onSurfaceVariant = SlateLight,
    error = RedLight,
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Red50,
    outline = SlateLight,
)

@Composable
fun TaskFlowAuditTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TaskFlowTypography,
        content = content
    )
}

// File: app/src/main/java/com/miui/dynamicisland/ui/theme/Theme.kt
// Purpose: Dark-only theme for Dynamic Island overlay (Apple HIG style)

package com.miui.dynamicisland.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dynamic Island overlay hamesha dark theme use karega – chahe system light mode me bhi ho
// Kyunki pill black background pe hi achi lagti hai (Apple bhi yahi karta hai)
private val DynamicIslandDarkColorScheme = darkColorScheme(
    primary = iOSBlue,
    onPrimary = IslandWhite,
    primaryContainer = IslandDarkGray,
    onPrimaryContainer = IslandWhite,

    secondary = iOSCyan,
    onSecondary = IslandBlack,
    secondaryContainer = IslandDarkGray,
    onSecondaryContainer = IslandWhite,

    tertiary = iOSPurple,
    onTertiary = IslandWhite,
    tertiaryContainer = IslandDarkGray,
    onTertiaryContainer = IslandWhite,

    background = IslandBlack,
    onBackground = IslandWhite,

    surface = IslandDarkGray,
    onSurface = IslandWhite,

    surfaceVariant = IslandGray,
    onSurfaceVariant = IslandLightGray,

    error = iOSRed,
    onError = IslandWhite,
    errorContainer = iOSRed.copy(alpha = 0.2f),
    onErrorContainer = iOSRed,

    outline = IslandGray,
    outlineVariant = IslandLightGray,

    scrim = IslandOverlayScrim
)

@Composable
fun DynamicIslandTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DynamicIslandDarkColorScheme,
        typography = DynamicIslandTypography,   // Type.kt se aata hai
        content = content
    )
}

// Preview ke liye – same theme
@Composable
fun PreviewDynamicIslandTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DynamicIslandDarkColorScheme,
        typography = DynamicIslandTypography,
        content = content
    )
}
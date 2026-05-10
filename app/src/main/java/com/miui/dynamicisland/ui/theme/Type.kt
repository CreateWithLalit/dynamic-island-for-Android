// File: app/src/main/java/com/miui/dynamicisland/ui/theme/Type.kt
// Purpose: Typography system matching Apple HIG (14-15sp for body, 13sp for pill text)

package com.miui.dynamicisland.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System default font – broad compatibility
val DefaultFontFamily = FontFamily.Default

// Apple HIG ke mutabik:
// - Body text: 14-15sp
// - Pill text (compact): 13-14sp
// - Secondary text: 11-12sp
// - Line heights accordingly

val DynamicIslandTypography = Typography(
    // Expanded view title (20sp – large)
    displayLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    // Song title / Notification app name (16sp)
    headlineMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Artist / Message preview (14sp – Apple HIG body)
    bodyLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Pill main text (e.g., "Charging", "Silent") – 13-14sp
    titleLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    // Battery %, duration (12sp)
    titleMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Small metadata (11sp)
    bodyMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp
    ),
    // Tiny labels (10sp)
    labelSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp
    ),
    // Status icons text (11sp)
    labelMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    // Countdown / timer numbers (14sp)
    labelLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

// Convenience alias
val IslandTextStyle = DynamicIslandTypography
val Typography = DynamicIslandTypography   // Backward compatibility
// File: app/src/main/java/com/miui/dynamicisland/ui/theme/Color.kt
// Purpose: iOS-accurate colors for Dynamic Island (Apple HIG compliant)

package com.miui.dynamicisland.ui.theme

import androidx.compose.ui.graphics.Color

// MARK: - iOS-Accurate Colors (Apple HIG)
// Ye colors exactly wahi hain jo Apple use karta hai – matlab island bilkul iOS jaisa lagega
val iOSGreen = Color(0xFF30D158)      // Charging / Call active / Success
val iOSRed = Color(0xFFFF3B30)        // Low battery / Error / End call
val iOSOrange = Color(0xFFFF9F0A)     // Silent / DND / Warning
val iOSBlue = Color(0xFF0A84FF)       // Bluetooth / Volume / Links
val iOSCyan = Color(0xFF64D2FF)       // Light blue accent
val iOSPurple = Color(0xFFBF5AF2)     // Social / Media accent
val iOSPink = Color(0xFFFF2D55)       // Media playback active
val iOSYellow = Color(0xFFFFD60A)     // Battery normal level

// MARK: - Dynamic Island Base Colors
val IslandBlack = Color(0xFF000000)       // Pure black background (OLED friendly)
val IslandDarkGray = Color(0xFF1C1C1E)    // Pill background (iOS style)
val IslandGray = Color(0xFF8E8E93)        // Secondary text
val IslandLightGray = Color(0xFFD1D1D6)   // Tertiary text
val IslandWhite = Color(0xFFFFFFFF)       // Primary text

// MARK: - Semantic Aliases (UI components me direct use karo)
val ChargingGreen = iOSGreen
val BatteryYellow = iOSYellow
val CriticalRed = iOSRed
val CallGreen = iOSGreen
val SilentOrange = iOSOrange
val VolumeBlue = iOSBlue
val BluetoothBlue = iOSBlue
val MediaPink = iOSPink

val MIUIBlue = iOSBlue      // MIUI compatibility mapped to iOS blue
val MIUICyan = iOSCyan
val MIUIPurple = iOSPurple

val IslandBackground = IslandDarkGray
val IslandOverlayScrim = Color(0x80000000)   // Semi-transparent black for expanded views
val IslandHighlight = Color(0x33FFFFFF)      // 20% white for touch feedback

val IslandTextPrimary = IslandWhite
val IslandTextSecondary = IslandLightGray
val IslandTextTertiary = IslandGray

// Status-specific
val BatteryChargingColor = iOSGreen
val BatteryLowColor = iOSRed
val BatteryNormalColor = iOSYellow
val NotificationRed = iOSRed
val CallActiveColor = iOSGreen
val MicMutedColor = iOSRed
val SpeakerOnColor = iOSBlue
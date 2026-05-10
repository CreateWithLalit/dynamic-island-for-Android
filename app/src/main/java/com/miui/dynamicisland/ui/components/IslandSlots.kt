// File: app/src/main/java/com/miui/dynamicisland/ui/components/IslandSlots.kt
// Purpose: Slot enums for all cutout-safe island widgets.
// Hinglish: Har widget ke liye LEFT/RIGHT slot define kiye hain.
//
// NOTE: WeatherSlot is defined in ui.island.DynamicIsland.kt (not here)
//       because WeatherWidget.kt imports it from ui.island package.

package com.miui.dynamicisland.ui.components

enum class ChargingSlot      { LEFT, RIGHT }
enum class SilentSlot        { LEFT, RIGHT }
enum class VolumeSlot        { LEFT, RIGHT }
enum class BluetoothSlot     { LEFT, RIGHT }
enum class CallSlot          { LEFT, RIGHT, BOTTOM }
enum class MediaSlot         { LEFT, RIGHT }
enum class NotificationSlot  { LEFT, RIGHT }
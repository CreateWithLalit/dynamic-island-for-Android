package com.miui.dynamicisland.data.repository

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.miui.dynamicisland.util.IslandLogger


object BluetoothBatteryNotificationParser {
    private val percentRegex = Regex("(\\d{1,3})%")

    fun tryUpdateFromNotification(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString() }
            .orEmpty()

        val combined = listOf(title, text, bigText, subText, lines)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        var left = extractLabeledPercent("left", combined)
        var right = extractLabeledPercent("right", combined)
        var caseLevel = extractLabeledPercent("case", combined)
        val overallFromLabel = extractLabeledPercent("battery", combined)

        val allPercents = percentRegex.findAll(combined)
            .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
            .filter { it in 0..100 }
            .toList()

        // Fallback for TWS (3 percentages: L, Case, R or L, R, Case)
        if (left == null && right == null && caseLevel == null) {
            if (allPercents.size >= 3) {
                left = allPercents[0]
                caseLevel = allPercents[1]
                right = allPercents[2]
            } else if (allPercents.size == 2) {
                left = allPercents[0]
                right = allPercents[1]
            }
        }

        val overall = overallFromLabel
            ?: if (allPercents.size == 1 && left == null && right == null && caseLevel == null) {
                allPercents.first()
            } else left ?: right ?: caseLevel

        val deviceName = resolveDeviceName(title, combined)
        
        // Return true if we found ANY battery info OR if we found a device name in a GMS notification
        val hasData = overall != null || left != null || right != null || caseLevel != null
        val isGmsBluetooth = (sbn.packageName == "com.google.android.gms" || sbn.packageName.contains("bluetooth", true)) 
                            && (deviceName != null || combined.contains("Buds", true) || combined.contains("Pods", true))

        IslandLogger.d("DEBUG_BT", """
            [DEBUG_BT] Parsing data for ${sbn.packageName}:
            - Combined: $combined
            - Device: $deviceName
            - Overall: $overall
            - Left: $left
            - Right: $right
            - Case: $caseLevel
            - Data found: $hasData, Is GMS BT: $isGmsBluetooth
        """.trimIndent(), null)

        if (!hasData && !isGmsBluetooth) return false

        BluetoothBatteryStore.update(
            BluetoothBatterySnapshot(
                deviceName = deviceName,
                batteryOverall = overall,
                batteryLeft = left,
                batteryRight = right,
                batteryCase = caseLevel
            )
        )
        return true
    }

    private fun resolveDeviceName(title: String, combined: String): String? {
        val candidate = if (title.isNotBlank()) title else combined
        val batteryIndex = candidate.indexOf("Battery", ignoreCase = true)
        val base = if (batteryIndex >= 0) candidate.substring(0, batteryIndex).trim() else candidate.trim()
        if (base.isBlank()) return null
        val cleaned = base.replace(Regex("(?i)^.*'s\\s+"), "").trim()
        return cleaned.ifBlank { null }
    }

    private fun extractLabeledPercent(label: String, text: String): Int? {
        val regex = Regex("(?i)$label\\s*[:\\-]?\\s*(\\d{1,3})%")
        return regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..100 }
    }
}

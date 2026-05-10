// File: app/src/main/java/com/miui/dynamicisland/data/model/AudioMode.kt
// Purpose: Represents audio/ringer mode for Dynamic Island – used by SilentWidget

package com.miui.dynamicisland.data.model

import androidx.annotation.Keep

@Keep
data class AudioMode(
    val ringerMode: RingerMode,
    val isDndEnabled: Boolean = false,
    val isMusicActive: Boolean = false,
    val streamVolume: Int = 0,
    val maxStreamVolume: Int = 15,
    val callVolume: Int = 0,
    val isHeadphonesConnected: Boolean = false,
    val isBluetoothScoOn: Boolean = false
) {

    enum class RingerMode {
        SILENT,   // Koi awaaz nahi, vibration bhi nahi
        VIBRATE,  // Sirf vibration
        NORMAL    // Normal ringtone
    }

    val isSilent: Boolean get() = ringerMode == RingerMode.SILENT
    val isVibrateOnly: Boolean get() = ringerMode == RingerMode.VIBRATE
    val isDoNotDisturb: Boolean get() = isDndEnabled

    // Apple HIG ke hisaab se pill me dikhne wala text
    val displayTitle: String
        get() = when {
            isDoNotDisturb -> "DND"
            isSilent -> "Silent"
            isVibrateOnly -> "Vibrate"
            else -> "Ring"
        }

    val iconName: String
        get() = when {
            isDoNotDisturb -> "dnd"
            isSilent -> "silent"
            isVibrateOnly -> "vibrate"
            else -> "ring"
        }

    // Apple HIG: Silent/DND = Orange (0xFFFF9F0A), Ring = Green (0xFF30D158)
    fun getModeColor(): Int {
        return when {
            isDoNotDisturb -> 0xFFFF9F0A.toInt()
            isSilent -> 0xFFFF9F0A.toInt()
            isVibrateOnly -> 0xFFFF9F0A.toInt()
            else -> 0xFF30D158.toInt()
        }
    }

    val volumePercent: Float
        get() = if (maxStreamVolume > 0) streamVolume.toFloat() / maxStreamVolume else 0f

    val formattedVolume: String
        get() = "$streamVolume/$maxStreamVolume"

    companion object {
        val DEFAULT = AudioMode(
            ringerMode = RingerMode.NORMAL,
            isDndEnabled = false,
            isMusicActive = false,
            streamVolume = 10,
            maxStreamVolume = 15,
            callVolume = 5
        )

        val SILENT_MODE = AudioMode(ringerMode = RingerMode.SILENT)
        val VIBRATE_MODE = AudioMode(ringerMode = RingerMode.VIBRATE)
        val DND_MODE = AudioMode(ringerMode = RingerMode.NORMAL, isDndEnabled = true)

        // Android ringer mode int (0 = silent, 1 = vibrate, 2 = normal) se create kare
        fun fromRingerModeInt(mode: Int): AudioMode {
            val ringerMode = when (mode) {
                0 -> RingerMode.SILENT
                1 -> RingerMode.VIBRATE
                else -> RingerMode.NORMAL
            }
            return AudioMode(ringerMode = ringerMode)
        }
    }
}
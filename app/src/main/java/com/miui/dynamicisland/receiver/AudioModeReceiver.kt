package com.miui.dynamicisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.miui.dynamicisland.util.IslandLogger

class AudioModeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AudioModeReceiver"
        const val ACTION_RINGER_MODE_CHANGED = "com.miui.dynamicisland.RINGER_MODE"
        const val ACTION_VOLUME_CHANGED = "com.miui.dynamicisland.VOLUME"
        const val ACTION_AUDIO_MODE_CHANGED = "com.miui.dynamicisland.AUDIO_REFRESH"

        const val EXTRA_RINGER_MODE = "extra_ringer_mode"
        const val EXTRA_STREAM_TYPE = "extra_stream_type"
        const val EXTRA_VOLUME = "extra_volume"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        IslandLogger.d(TAG, "Received: $action", null)

        when (action) {
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val ringerMode = audioManager.ringerMode
                context.sendBroadcast(Intent(ACTION_RINGER_MODE_CHANGED).apply {
                    putExtra(EXTRA_RINGER_MODE, ringerMode)
                    setPackage(context.packageName)
                })
            }
            "android.media.VOLUME_CHANGED_ACTION" -> {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                val volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                context.sendBroadcast(Intent(ACTION_VOLUME_CHANGED).apply {
                    putExtra(EXTRA_STREAM_TYPE, streamType)
                    putExtra(EXTRA_VOLUME, volume)
                    setPackage(context.packageName)
                })
            }
            "android.media.STREAM_DEVICES_CHANGED_ACTION",
            "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION" -> {
                context.sendBroadcast(Intent(ACTION_AUDIO_MODE_CHANGED).apply {
                    setPackage(context.packageName)
                })
            }
        }
    }
}
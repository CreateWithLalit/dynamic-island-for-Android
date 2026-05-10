// File: app/src/main/java/com/miui/dynamicisland/data/repository/AudioRepository.kt

package com.miui.dynamicisland.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.data.model.AudioMode
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
private const val STREAM_DEVICES_CHANGED_ACTION = "android.media.STREAM_DEVICES_CHANGED_ACTION"
private const val INTERNAL_RINGER_MODE_CHANGED_ACTION = "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"

class AudioRepository(private val context: Context) {

    companion object {
        private const val TAG = "AudioRepository"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = NotificationManagerCompat.from(context)

    private val _volumeButtonPressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val volumeButtonPressed: SharedFlow<Unit> = _volumeButtonPressed.asSharedFlow()

    val audioMode: StateFlow<AudioMode?> = callbackFlow {
        IslandLogger.d(TAG, "Subscribing to audio mode updates", null)

        val headphoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                    val isPlugged = intent.getIntExtra("state", 0) == 1
                    IslandLogger.d(TAG, "Headphone state changed: plugged=$isPlugged", null)
                    trySend(getCurrentAudioMode())
                }
            }
        }

        val audioReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    VOLUME_CHANGED_ACTION,
                    STREAM_DEVICES_CHANGED_ACTION,
                    AudioManager.RINGER_MODE_CHANGED_ACTION,
                    INTERNAL_RINGER_MODE_CHANGED_ACTION -> {
                        IslandLogger.d(TAG, "Audio event: ${intent.action}", null)
                        if (intent.action == VOLUME_CHANGED_ACTION) {
                            _volumeButtonPressed.tryEmit(Unit)
                        }
                        trySend(getCurrentAudioMode())
                    }
                }
            }
        }

        val audioFilter = IntentFilter().apply {
            addAction(VOLUME_CHANGED_ACTION)
            addAction(STREAM_DEVICES_CHANGED_ACTION)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addAction(INTERNAL_RINGER_MODE_CHANGED_ACTION)
            }
        }
        val headphoneFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)

        try {
            ContextCompat.registerReceiver(
                context.applicationContext,
                audioReceiver,
                audioFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            ContextCompat.registerReceiver(
                context.applicationContext,
                headphoneReceiver,
                headphoneFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            IslandLogger.d(TAG, "Audio receivers registered", null)
            trySend(getCurrentAudioMode())
        } catch (e: Exception) {
            // FIXED: Passing actual Exception object as the third parameter
            IslandLogger.e(TAG, "Error registering audio receivers", e)
            trySend(AudioMode.DEFAULT)
        }

        awaitClose {
            IslandLogger.d(TAG, "Unsubscribing from audio mode updates", null)
            try {
                context.applicationContext.unregisterReceiver(audioReceiver)
                context.applicationContext.unregisterReceiver(headphoneReceiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            } catch (e: Exception) {
                // FIXED: Passing actual Exception object as the third parameter
                IslandLogger.e(TAG, "Error unregistering audio receivers", e)
            }
        }
    }.distinctUntilChanged { old, new ->
        old?.ringerMode == new?.ringerMode &&
                old?.isDndEnabled == new?.isDndEnabled &&
                old?.streamVolume == new?.streamVolume &&
                old?.isHeadphonesConnected == new?.isHeadphonesConnected &&
                old?.isBluetoothScoOn == new?.isBluetoothScoOn
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val ringerMode: Flow<AudioMode.RingerMode> = audioMode.map { it?.ringerMode ?: AudioMode.RingerMode.NORMAL }
    val isDndEnabled: Flow<Boolean> = audioMode.map { it?.isDndEnabled ?: false }
    val volumePercent: Flow<Float> = audioMode.map { it?.volumePercent ?: 0f }

    private fun getCurrentAudioMode(): AudioMode {
        val ringerModeInt = audioManager.ringerMode
        val ringerMode = when (ringerModeInt) {
            AudioManager.RINGER_MODE_SILENT -> AudioMode.RingerMode.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> AudioMode.RingerMode.VIBRATE
            else -> AudioMode.RingerMode.NORMAL
        }

        val dndPolicy = notificationManager.currentInterruptionFilter
        val isDndEnabled = dndPolicy == NotificationManagerCompat.INTERRUPTION_FILTER_NONE ||
                dndPolicy == NotificationManagerCompat.INTERRUPTION_FILTER_ALARMS ||
                dndPolicy == NotificationManagerCompat.INTERRUPTION_FILTER_PRIORITY

        val isMusicActive = audioManager.isMusicActive
        val streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val callVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)

        var isHeadphonesConnected = false
        var isBluetoothScoOn = false
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> isHeadphonesConnected = true
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    isBluetoothScoOn = true
                    isHeadphonesConnected = true
                }
            }
        }

        return AudioMode(
            ringerMode = ringerMode,
            isDndEnabled = isDndEnabled,
            isMusicActive = isMusicActive,
            streamVolume = streamVolume,
            maxStreamVolume = maxStreamVolume,
            callVolume = callVolume,
            isHeadphonesConnected = isHeadphonesConnected,
            isBluetoothScoOn = isBluetoothScoOn
        )
    }

    fun refresh() {
        IslandLogger.d(TAG, "Manual refresh requested", null)
    }
}
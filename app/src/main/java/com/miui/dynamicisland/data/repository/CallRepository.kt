package com.miui.dynamicisland.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.data.model.CallState
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class CallRepository(private val context: Context) {

    companion object {
        private const val TAG = "CallRepository"
    }

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null
    private val _ongoingDuration = MutableStateFlow(0L)
    val ongoingDuration: StateFlow<Long> = _ongoingDuration.asStateFlow()

    val callState: Flow<CallState> = callbackFlow {
        // 1. Permission Check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            IslandLogger.w(TAG, "Missing READ_PHONE_STATE permission", null)
            trySend(CallState.Idle)
            awaitClose { }
            return@callbackFlow
        }

        val stateCallback = { state: Int, phoneNumber: String? ->
            val newState = mapState(state, phoneNumber)
            IslandLogger.d(TAG, "Call State Changed: $state", null)
            
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                startDurationTimer()
            } else {
                stopDurationTimer()
            }
            
            trySend(newState)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = @RequiresApi(Build.VERSION_CODES.S)
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    stateCallback(state, null)
                }
            }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            awaitClose { telephonyManager.unregisterTelephonyCallback(callback) }
        } else {
            val listener = object : android.telephony.PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    stateCallback(state, phoneNumber)
                }
            }
            telephonyManager.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            awaitClose { telephonyManager.listen(listener, android.telephony.PhoneStateListener.LISTEN_NONE) }
        }
    }.distinctUntilChanged()

    private fun startDurationTimer() {
        durationJob?.cancel()
        _ongoingDuration.value = 0L
        durationJob = repositoryScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                _ongoingDuration.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    private fun mapState(state: Int, phoneNumber: String?): CallState {
        return when (state) {
            TelephonyManager.CALL_STATE_RINGING -> CallState.Ringing(phoneNumber)
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OffHook
            else -> CallState.Idle
        }
    }

    // Interactive Actions
    fun acceptCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager?.acceptRingingCall()
            }
            IslandLogger.d(TAG, "Accept call requested", null)
        } catch (e: SecurityException) {
            IslandLogger.e(TAG, "No permission to accept call", e)
        }
    }

    fun declineCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecomManager?.endCall()
            }
            IslandLogger.d(TAG, "Decline call requested", null)
        } catch (e: SecurityException) {
            IslandLogger.e(TAG, "No permission to end call", e)
        }
    }

    fun endCall() {
        declineCall()
    }

    fun toggleMute() {
        val currentMute = audioManager.isMicrophoneMute
        audioManager.isMicrophoneMute = !currentMute
        IslandLogger.d(TAG, "Microphone mute toggled: ${!currentMute}", null)
    }
}

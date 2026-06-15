package com.miui.dynamicisland.service

import android.content.Context
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.miui.dynamicisland.data.repository.CallRepository
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.*

/**
 * Service to handle calls when the app is set as the Default Phone App.
 * Hinglish: Jab app Default Dialer hota hai, tab ye service calls ko control karti hai.
 */
class IslandCallService : InCallService() {

    companion object {
        private const val TAG = "IslandCallService"
        private var currentCall: Call? = null
        private var instance: IslandCallService? = null

        fun getActiveCall(): Call? = currentCall

        fun isSpeakerOn(): Boolean {
            return instance?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
        }

        fun isMuted(): Boolean {
            return instance?.callAudioState?.isMuted ?: false
        }

        fun setSpeaker(on: Boolean) {
            val route = if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            IslandLogger.d(TAG, "Requesting audio route: ${if (on) "SPEAKER" else "EARPIECE"}", null)
            if (instance == null) {
                IslandLogger.w(TAG, "Cannot set audio route: IslandCallService instance is null (App might not be Default Dialer)", null)
                return
            }
            try {
                instance?.setAudioRoute(route)
                IslandLogger.d(TAG, "setAudioRoute($route) called successfully", null)
            } catch (e: Exception) {
                IslandLogger.e(TAG, "Failed to set audio route", e)
            }
        }

        fun setMute(on: Boolean) {
            IslandLogger.d(TAG, "Requesting mute: $on", null)
            if (instance == null) {
                IslandLogger.w(TAG, "Cannot set mute: IslandCallService instance is null", null)
                return
            }
            try {
                instance?.setMuted(on)
                IslandLogger.d(TAG, "setMuted($on) called successfully", null)
            } catch (e: Exception) {
                IslandLogger.e(TAG, "Failed to set mute", e)
            }
        }
    }

    private lateinit var callRepo: CallRepository
    private val stateManager = IslandStateManager.getInstance()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null
    private var ongoingDuration = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        callRepo = CallRepository(applicationContext)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        IslandLogger.d(TAG, "Call added: ${call.details.handle}", null)
        currentCall = call
        
        updateIslandState(call)

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                IslandLogger.d(TAG, "Call state changed: $state", null)
                
                if (state == Call.STATE_ACTIVE) {
                    startDurationTimer(call)
                } else if (state == Call.STATE_DISCONNECTED) {
                    stopDurationTimer()
                }
                
                updateIslandState(call)
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        IslandLogger.d(TAG, "Call removed", null)
        if (currentCall == call) {
            currentCall = null
            stopDurationTimer()
            stateManager.removeState(IslandState.Call::class)
        }
    }

    private fun startDurationTimer(call: Call) {
        durationJob?.cancel()
        ongoingDuration = 0L
        durationJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                ongoingDuration = System.currentTimeMillis() - startTime
                updateIslandState(call)
                delay(1000)
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        IslandLogger.d(TAG, "Audio state changed: route=${routeToString(audioState.route)}, muted=${audioState.isMuted}", null)
        currentCall?.let { updateIslandState(it) }
    }

    private fun routeToString(route: Int): String {
        return when (route) {
            CallAudioState.ROUTE_EARPIECE -> "EARPIECE"
            CallAudioState.ROUTE_SPEAKER -> "SPEAKER"
            CallAudioState.ROUTE_WIRED_HEADSET -> "WIRED_HEADSET"
            CallAudioState.ROUTE_BLUETOOTH -> "BLUETOOTH"
            else -> "UNKNOWN ($route)"
        }
    }

    private fun updateIslandState(call: Call) {
        val number = call.details.handle?.schemeSpecificPart
        val name = callRepo.getContactName(number)
        val photo = callRepo.getContactPhoto(number)
        
        val state = call.state
        val isIncoming = state == Call.STATE_RINGING || state == Call.STATE_CONNECTING
        val isOngoing = state == Call.STATE_ACTIVE || state == Call.STATE_DIALING
        
        if (state == Call.STATE_DISCONNECTED) {
            stateManager.removeState(IslandState.Call::class)
            return
        }

        val currentState = stateManager.currentState.value
        val wasExpanded = if (currentState is IslandState.Call) {
            currentState.isExpanded
        } else {
            isIncoming // Auto-expand for incoming, start compact for others
        }

        val audioState = callAudioState
        val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
        val isMuted = audioState?.isMuted ?: false

        stateManager.pushState(
            IslandState.Call(
                callerName = name ?: number ?: "Unknown",
                callerSubtext = if (name != null) number ?: "" else "Incoming Call",
                callerPhoto = photo,
                isIncoming = isIncoming,
                isOngoing = isOngoing,
                isSpeakerOn = isSpeakerOn,
                isMuted = isMuted,
                duration = ongoingDuration,
                isExpanded = wasExpanded
            )
        )
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

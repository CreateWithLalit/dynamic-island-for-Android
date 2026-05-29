package com.miui.dynamicisland.service

import android.content.Context
import android.telecom.Call
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

        fun getActiveCall(): Call? = currentCall
    }

    private lateinit var callRepo: CallRepository
    private val stateManager = IslandStateManager.getInstance()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null
    private var ongoingDuration = 0L

    override fun onCreate() {
        super.onCreate()
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

        stateManager.pushState(
            IslandState.Call(
                callerName = name ?: number ?: "Unknown",
                callerSubtext = if (name != null) number ?: "" else "Incoming Call",
                callerPhoto = photo,
                isIncoming = isIncoming,
                isOngoing = isOngoing,
                duration = ongoingDuration,
                isExpanded = isIncoming // Auto expand on incoming
            )
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

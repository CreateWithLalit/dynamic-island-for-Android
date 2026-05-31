package com.miui.dynamicisland.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.data.model.CallState
import com.miui.dynamicisland.service.IslandAccessibilityService
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

    fun getContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    fun getContactPhoto(phoneNumber: String?): android.graphics.Bitmap? {
        if (phoneNumber.isNullOrBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.PHOTO_URI)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val photoUriString = cursor.getString(0)
                if (photoUriString != null) {
                    val photoUri = android.net.Uri.parse(photoUriString)
                    context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                        return android.graphics.BitmapFactory.decodeStream(inputStream)
                    }
                }
            }
        }
        return null
    }

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
        // Send command to External Dialer first
        val intent = Intent("com.miui.dynamicisland.ACTION_ANSWER")
        intent.`package` = "com.goodwy.dialer" // Update this if your dialer package is different
        context.sendBroadcast(intent)

        try {
            // Method 0: If we are the default dialer
            val activeCall = com.miui.dynamicisland.service.IslandCallService.getActiveCall()
            if (activeCall != null) {
                activeCall.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                IslandLogger.d(TAG, "Call accepted via IslandCallService", null)
                return
            }

            // Method 1: Accessibility (best for MIUI)
            val done = IslandAccessibilityService.acceptCall()
            if (done) {
                IslandLogger.d(TAG, "Call accepted via accessibility", null)
                return
            }
            
            // Method 2: AudioManager via headset hook fallback
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
            )
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
            )
            IslandLogger.d(TAG, "Accept via headsethook fallback", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Accept call failed", e)
        }
    }

    fun declineCall() {
        // Send command to External Dialer first
        val intent = Intent("com.miui.dynamicisland.ACTION_DECLINE")
        intent.`package` = "com.goodwy.dialer"
        context.sendBroadcast(intent)

        try {
            // Method 0: If we are the default dialer
            val activeCall = com.miui.dynamicisland.service.IslandCallService.getActiveCall()
            if (activeCall != null) {
                activeCall.disconnect()
                IslandLogger.d(TAG, "Call declined via IslandCallService", null)
                return
            }

            // Method 1: Accessibility
            val done = IslandAccessibilityService.declineCall()
            if (done) {
                IslandLogger.d(TAG, "Call declined via accessibility", null)
                return
            }
            
            // Method 2: TelecomManager fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val result = telecomManager?.endCall()
                IslandLogger.d(TAG, "Decline via telecom fallback result: $result", null)
            }
            
            // Intent Fallback
            val intent = Intent(Intent.ACTION_ANSWER)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        } catch (e: SecurityException) {
            IslandLogger.e(TAG, "No permission to decline", e)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Decline call failed", e)
        }
    }

    fun endCall() {
        declineCall()
    }

    @SuppressLint("MissingPermission")
    fun placeCall(number: String) {
        try {
            val uri = android.net.Uri.fromParts("tel", number, null)
            telecomManager?.placeCall(uri, null)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$number"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun toggleMute() {
        val currentMute = audioManager.isMicrophoneMute
        audioManager.isMicrophoneMute = !currentMute
        IslandLogger.d(TAG, "Microphone mute toggled: ${!currentMute}", null)
    }
}

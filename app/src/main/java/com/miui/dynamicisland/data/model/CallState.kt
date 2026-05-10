package com.miui.dynamicisland.data.model

sealed class CallState {
    object Idle : CallState()
    object OffHook : CallState()
    data class Ringing(val phoneNumber: String?) : CallState()
}

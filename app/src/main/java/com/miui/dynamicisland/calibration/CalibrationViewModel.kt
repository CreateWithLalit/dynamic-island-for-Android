// File: app/src/main/java/com/miui/dynamicisland/calibration/CalibrationViewModel.kt
// Purpose: Manages calibration state and persistence via CalibrationManager.
// Hinglish: Is ViewModel mein calibration state manage hoti hai aur DataStore mein save hoti hai.
//
// FIX: updateCalibration ab har slider change pe call hota hai → overlay real-time update hota hai.
//      saveCalibration sirf final "Save" button pe call karo (already correct).

package com.miui.dynamicisland.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miui.dynamicisland.manager.CalibrationManager
import com.miui.dynamicisland.manager.IslandCalibration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalibrationViewModel(
    private val calibrationManager: CalibrationManager
) : ViewModel() {

    private val _calibration = MutableStateFlow(IslandCalibration.default())
    val calibration: StateFlow<IslandCalibration> = _calibration.asStateFlow()

    init {
        viewModelScope.launch {
            calibrationManager.calibration.collect { newCal ->
                _calibration.value = newCal
            }
        }
    }

    // ── Real-time updates (called on every slider/drag event) ─────────────────

    fun updateOffsets(x: Float, y: Float) {
        viewModelScope.launch {
            calibrationManager.updatePosition(x, y)
        }
    }

    fun updateCornerRadius(radius: Float) {
        viewModelScope.launch {
            calibrationManager.updateCalibration(_calibration.value.copy(cornerRadius = radius))
        }
    }

    fun updatePillWidth(width: Float) {
        viewModelScope.launch {
            calibrationManager.updateCalibration(_calibration.value.copy(pillWidth = width))
        }
    }

    fun updatePillHeight(height: Float) {
        viewModelScope.launch {
            calibrationManager.updateCalibration(_calibration.value.copy(pillHeight = height))
        }
    }

    fun updateFixedMode(isFixed: Boolean) {
        viewModelScope.launch {
            calibrationManager.updateCalibration(_calibration.value.copy(isFixed = isFixed))
        }
    }

    // ── Save (final sync) & Reset ─────────────────────────────────────────────

    fun saveCalibration() {
        viewModelScope.launch {
            calibrationManager.updateCalibration(_calibration.value)
        }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            calibrationManager.resetToDefaults()
        }
    }
}
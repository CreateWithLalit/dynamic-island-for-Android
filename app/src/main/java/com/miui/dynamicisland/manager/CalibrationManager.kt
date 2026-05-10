package com.miui.dynamicisland.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "island_calibration"
)

class CalibrationManager(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "CalibrationManager"

        val KEY_OFFSET_X = floatPreferencesKey("offset_x")
        val KEY_OFFSET_Y = floatPreferencesKey("offset_y")
        val KEY_CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val KEY_PILL_WIDTH = floatPreferencesKey("pill_width")
        val KEY_PILL_HEIGHT = floatPreferencesKey("pill_height")
        val KEY_NOTCH_TYPE = intPreferencesKey("notch_type")
        val KEY_IS_FIXED = booleanPreferencesKey("is_fixed")

        const val DEFAULT_OFFSET_X = 0f
        const val DEFAULT_OFFSET_Y = -36f
        const val DEFAULT_CORNER_RADIUS = 18.5f
        const val DEFAULT_PILL_WIDTH = 126f
        const val DEFAULT_PILL_HEIGHT = 37f

        const val NOTCH_TYPE_NONE = 0
        const val NOTCH_TYPE_CENTER_PILL = 1
        const val NOTCH_TYPE_WATERDROP = 2
        const val NOTCH_TYPE_FIXED = 3
    }

    val calibration: Flow<IslandCalibration> = appContext.dataStore.data
        .catch { exception ->
            IslandLogger.e(TAG, "Error reading calibration from DataStore", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            IslandCalibration(
                offsetX = preferences[KEY_OFFSET_X] ?: DEFAULT_OFFSET_X,
                offsetY = preferences[KEY_OFFSET_Y] ?: DEFAULT_OFFSET_Y,
                cornerRadius = preferences[KEY_CORNER_RADIUS] ?: DEFAULT_CORNER_RADIUS,
                pillWidth = preferences[KEY_PILL_WIDTH] ?: DEFAULT_PILL_WIDTH,
                pillHeight = preferences[KEY_PILL_HEIGHT] ?: DEFAULT_PILL_HEIGHT,
                notchType = preferences[KEY_NOTCH_TYPE] ?: NOTCH_TYPE_CENTER_PILL,
                isFixed = preferences[KEY_IS_FIXED] ?: true
            )
        }

    suspend fun updateCalibration(calibration: IslandCalibration) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[KEY_OFFSET_X] = calibration.offsetX
                preferences[KEY_OFFSET_Y] = calibration.offsetY
                preferences[KEY_CORNER_RADIUS] = calibration.cornerRadius
                preferences[KEY_PILL_WIDTH] = calibration.pillWidth
                preferences[KEY_PILL_HEIGHT] = calibration.pillHeight
                preferences[KEY_NOTCH_TYPE] = calibration.notchType
                preferences[KEY_IS_FIXED] = calibration.isFixed
            }
            IslandLogger.d(TAG, "Calibration updated: offset=(${calibration.offsetX}, ${calibration.offsetY})", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Failed to update calibration: ${e.message}", e)
        }
    }

    suspend fun resetToDefaults() {
        updateCalibration(IslandCalibration.default())
        IslandLogger.d(TAG, "Calibration reset to defaults", null)
    }

    suspend fun updatePosition(offsetX: Float, offsetY: Float) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[KEY_OFFSET_X] = offsetX
                preferences[KEY_OFFSET_Y] = offsetY
            }
            IslandLogger.d(TAG, "Position updated: ($offsetX, $offsetY)", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Failed to update position: ${e.message}", e)
        }
    }

    fun getNotchTypeName(notchType: Int): String {
        return when (notchType) {
            NOTCH_TYPE_NONE -> "None"
            NOTCH_TYPE_CENTER_PILL -> "Center Punch-hole"
            NOTCH_TYPE_WATERDROP -> "Waterdrop"
            else -> "Unknown"
        }
    }
}

data class IslandCalibration(
    val offsetX: Float,
    val offsetY: Float,
    val cornerRadius: Float,
    val pillWidth: Float,
    val pillHeight: Float,
    val notchType: Int,
    val isFixed: Boolean = true
) {
    fun withOffset(x: Float = offsetX, y: Float = offsetY): IslandCalibration = copy(offsetX = x, offsetY = y)

    companion object {
        fun default(): IslandCalibration = IslandCalibration(
            offsetX = CalibrationManager.DEFAULT_OFFSET_X,
            offsetY = CalibrationManager.DEFAULT_OFFSET_Y,
            cornerRadius = CalibrationManager.DEFAULT_CORNER_RADIUS,
            pillWidth = CalibrationManager.DEFAULT_PILL_WIDTH,
            pillHeight = CalibrationManager.DEFAULT_PILL_HEIGHT,
            notchType = CalibrationManager.NOTCH_TYPE_CENTER_PILL
        )
    }
}
// File: app/src/main/java/com/miui/dynamicisland/manager/IslandSizeManager.kt
// Purpose: Stores custom dimensions per state (user overrides). Defaults = Apple HIG.

package com.miui.dynamicisland.manager

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

data class FeatureSizeConfig(
    val featureName: String,
    val expandedHeightScale: Float
)

data class IslandDimensions(
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp
)

/** Nullable override values; when a field is null, base sizing logic should be used. */
data class IslandDimensionsOverride(
    val width: Dp? = null,
    val height: Dp? = null,
    val cornerRadius: Dp? = null,
    val expandedHeightScale: Float? = null
)

private val Context.islandSizeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "island_dimensions"
)

private val EXPANDED_WIDTH_SCALE_KEY = floatPreferencesKey("expanded_width_scale")
private val EXPANDED_HEIGHT_SCALE_KEY = floatPreferencesKey("expanded_height_scale")

open class IslandSizeManager(
    context: Context
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // Defaults aligned with current DynamicIsland compact sizing (so enabling overrides won't change UI).
    // Note: Some states also use global CalibrationManager values at runtime.
    private val defaultDimensions: Map<KClass<out IslandState>, IslandDimensions> = mapOf(
        IslandState.Idle::class         to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Charging::class     to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Call::class         to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Weather::class      to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Volume::class       to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Media::class        to IslandDimensions(220.dp, 37.dp, 18.5.dp),
        IslandState.Notification::class to IslandDimensions(215.dp, 37.dp, 18.5.dp),
        IslandState.Bluetooth::class    to IslandDimensions(220.dp, 37.dp, 18.5.dp),
        IslandState.Silent::class       to IslandDimensions(160.dp, 37.dp, 18.5.dp)
    )

    // Expanded dimensions are handled separately in DynamicIsland logic,
    // but we keep them here for potential overrides.
    // For now, we only store compact dimensions.

    private val _currentDimensions = MutableStateFlow(defaultDimensions)
    val dimensionsFlow: StateFlow<Map<KClass<out IslandState>, IslandDimensions>> =
        _currentDimensions.asStateFlow()

    private val _expandedWidthScale = MutableStateFlow(1f)
    val expandedWidthScaleFlow: StateFlow<Float> = _expandedWidthScale.asStateFlow()

    private val _expandedHeightScale = MutableStateFlow(1f)
    val expandedHeightScaleFlow: StateFlow<Float> = _expandedHeightScale.asStateFlow()

    private val _overrides = MutableStateFlow<Map<KClass<out IslandState>, IslandDimensionsOverride>>(emptyMap())
    val overridesFlow: StateFlow<Map<KClass<out IslandState>, IslandDimensionsOverride>> =
        _overrides.asStateFlow()

    init {
        scope.launch {
            appContext.islandSizeDataStore.data
                .catch { exception ->
                    IslandLogger.e("IslandSizeManager", "Failed to read dimensions", exception)
                    emit(emptyPreferences())
                }
                .collect { preferences ->
                    _currentDimensions.value = defaultDimensions.mapValues { entry ->
                        val stateName = entry.key.simpleName.orEmpty().lowercase()
                        val default = entry.value

                        val width = preferences[floatPreferencesKey("width_$stateName")]
                        val height = preferences[floatPreferencesKey("height_$stateName")]
                        val radius = preferences[floatPreferencesKey("radius_$stateName")]

                        IslandDimensions(
                            width = if (width != null && width > 0f) width.dp else default.width,
                            height = if (height != null && height > 0f) height.dp else default.height,
                            cornerRadius = if (radius != null && radius > 0f) radius.dp else default.cornerRadius
                        )
                    }

                    _expandedWidthScale.value = preferences[EXPANDED_WIDTH_SCALE_KEY]?.takeIf { it > 0f } ?: 1f
                    _expandedHeightScale.value = preferences[EXPANDED_HEIGHT_SCALE_KEY]?.takeIf { it > 0f } ?: 1f

                    // Keep raw overrides (nullable) so runtime can apply them on top of calibration/base logic.
                    _overrides.value = defaultDimensions.keys.associateWith { kClass ->
                        val stateName = kClass.simpleName.orEmpty().lowercase()
                        val width = preferences[floatPreferencesKey("width_$stateName")]
                        val height = preferences[floatPreferencesKey("height_$stateName")]
                        val radius = preferences[floatPreferencesKey("radius_$stateName")]
                        val expHeightScale = preferences[floatPreferencesKey("exp_height_scale_$stateName")]

                        IslandDimensionsOverride(
                            width = width?.takeIf { it > 0f }?.dp,
                            height = height?.takeIf { it > 0f }?.dp,
                            cornerRadius = radius?.takeIf { it > 0f }?.dp,
                            expandedHeightScale = expHeightScale?.takeIf { it > 0f }
                        )
                    }
                }
        }
    }

    open fun getDimensionsForState(state: IslandState): IslandDimensions {
        return _currentDimensions.value[state::class]
            ?: defaultDimensions[state::class]
            ?: IslandDimensions(126.dp, 37.dp, 18.5.dp) // fallback to compact
    }

    fun getExpandedHeightScaleForState(stateClass: KClass<out IslandState>): Float {
        return _overrides.value[stateClass]?.expandedHeightScale ?: _expandedHeightScale.value
    }

    fun getDefaultDimensionsForState(stateClass: KClass<out IslandState>): IslandDimensions {
        return defaultDimensions[stateClass] ?: IslandDimensions(126.dp, 37.dp, 18.5.dp)
    }

    fun getOverrideForState(stateClass: KClass<out IslandState>): IslandDimensionsOverride {
        return _overrides.value[stateClass] ?: IslandDimensionsOverride()
    }

    fun getAllFeatureSizeConfigs(): List<FeatureSizeConfig> {
        return defaultDimensions.keys.map { kClass ->
            FeatureSizeConfig(
                featureName = kClass.simpleName ?: "Unknown",
                expandedHeightScale = getExpandedHeightScaleForState(kClass)
            )
        }
    }

    suspend fun updateDimensions(
        state: IslandState,
        width: Float? = null,
        height: Float? = null,
        cornerRadius: Float? = null
    ) {
        updateDimensions(state::class, width, height, cornerRadius)
    }

    suspend fun updateDimensions(
        stateClass: KClass<out IslandState>,
        width: Float? = null,
        height: Float? = null,
        cornerRadius: Float? = null,
        expandedHeightScale: Float? = null
    ) {
        val stateName = stateClass.simpleName.orEmpty().lowercase()
        if (stateName.isBlank()) return

        appContext.islandSizeDataStore.edit { preferences ->
            val widthKey = floatPreferencesKey("width_$stateName")
            val heightKey = floatPreferencesKey("height_$stateName")
            val radiusKey = floatPreferencesKey("radius_$stateName")
            val expHeightScaleKey = floatPreferencesKey("exp_height_scale_$stateName")

            if (width != null) {
                if (width > 0f) preferences[widthKey] = width else preferences.remove(widthKey)
            }
            if (height != null) {
                if (height > 0f) preferences[heightKey] = height else preferences.remove(heightKey)
            }
            if (cornerRadius != null) {
                if (cornerRadius > 0f) preferences[radiusKey] = cornerRadius else preferences.remove(radiusKey)
            }
            if (expandedHeightScale != null) {
                if (expandedHeightScale > 0f) preferences[expHeightScaleKey] = expandedHeightScale else preferences.remove(expHeightScaleKey)
            }
        }
    }

    suspend fun updateExpandedWidthScale(scale: Float) {
        appContext.islandSizeDataStore.edit { preferences ->
            if (scale > 0f && scale != 1f) {
                preferences[EXPANDED_WIDTH_SCALE_KEY] = scale
            } else {
                preferences.remove(EXPANDED_WIDTH_SCALE_KEY)
            }
        }
    }

    suspend fun updateExpandedHeightScale(scale: Float) {
        appContext.islandSizeDataStore.edit { preferences ->
            if (scale > 0f && scale != 1f) {
                preferences[EXPANDED_HEIGHT_SCALE_KEY] = scale
            } else {
                preferences.remove(EXPANDED_HEIGHT_SCALE_KEY)
            }
        }
    }

    suspend fun clearOverridesForState(stateClass: KClass<out IslandState>) {
        updateDimensions(stateClass, width = 0f, height = 0f, cornerRadius = 0f, expandedHeightScale = 0f)
    }

    suspend fun resetAllOverrides() {
        appContext.islandSizeDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

private var INSTANCE: IslandSizeManager? = null

fun getIslandSizeManager(context: Context): IslandSizeManager {
    return INSTANCE ?: synchronized(IslandSizeManager::class) {
        INSTANCE ?: IslandSizeManager(context.applicationContext).also { manager ->
            INSTANCE = manager
        }
    }
}
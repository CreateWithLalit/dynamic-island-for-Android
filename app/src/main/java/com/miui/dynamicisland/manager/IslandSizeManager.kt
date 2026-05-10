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

data class IslandDimensions(
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp
)

private val Context.islandSizeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "island_dimensions"
)

open class IslandSizeManager(
    context: Context
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // Apple HIG default dimensions per state (compact = 126x37, expanded = 371x84 or larger)
    private val defaultDimensions: Map<KClass<out IslandState>, IslandDimensions> = mapOf(
        // Compact states (126dp x 37dp, radius 18.5dp)
        IslandState.Idle::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Charging::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Silent::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Volume::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Bluetooth::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        // Notification compact (but may be expanded)
        IslandState.Notification::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        // Media compact
        IslandState.Media::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        // Call compact
        IslandState.Call::class to IslandDimensions(126.dp, 37.dp, 18.5.dp),
        IslandState.Weather::class to IslandDimensions(126.dp, 37.dp, 18.5.dp)
    )

    // Expanded dimensions are handled separately in DynamicIsland logic,
    // but we keep them here for potential overrides.
    // For now, we only store compact dimensions.

    private val _currentDimensions = MutableStateFlow(defaultDimensions)
    val dimensionsFlow: StateFlow<Map<KClass<out IslandState>, IslandDimensions>> =
        _currentDimensions.asStateFlow()

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
                }
        }
    }

    open fun getDimensionsForState(state: IslandState): IslandDimensions {
        return _currentDimensions.value[state::class]
            ?: defaultDimensions[state::class]
            ?: IslandDimensions(126.dp, 37.dp, 18.5.dp) // fallback to compact
    }

    suspend fun updateDimensions(
        state: IslandState,
        width: Float? = null,
        height: Float? = null,
        cornerRadius: Float? = null
    ) {
        appContext.islandSizeDataStore.edit { preferences ->
            val stateName = state::class.simpleName.orEmpty().lowercase()
            if (stateName.isBlank()) return@edit

            width?.let { preferences[floatPreferencesKey("width_$stateName")] = it }
            height?.let { preferences[floatPreferencesKey("height_$stateName")] = it }
            cornerRadius?.let { preferences[floatPreferencesKey("radius_$stateName")] = it }
        }
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
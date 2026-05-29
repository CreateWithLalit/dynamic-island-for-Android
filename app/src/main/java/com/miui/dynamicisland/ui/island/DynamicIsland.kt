// File: app/src/main/java/com/miui/dynamicisland/ui/island/DynamicIsland.kt
// Purpose: Root composable for the island overlay.
//          Decides which widget to show, handles tap→expand/collapse.
// Hinglish: Yahan se poora island control hota hai – state ke hisab se widget dikhta hai
//           aur tap se expand/collapse hota hai.

package com.miui.dynamicisland.ui.island

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.miui.dynamicisland.manager.IslandCalibration
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.manager.getIslandSizeManager
import com.miui.dynamicisland.ui.components.*
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.ui.states.withExpanded

// ─── Sealed action type for media controls ────────────────────────────────────
sealed class MediaAction {
    object PlayPause : MediaAction()
    object Next      : MediaAction()
    object Previous  : MediaAction()
    data class Seek(val position: Float) : MediaAction()   // 0f–1f progress fraction
}

// ─── Sealed action type for call controls ─────────────────────────────────────
sealed class CallAction {
    object Accept  : CallAction()
    object Decline : CallAction()
    object End     : CallAction()
    object Mute    : CallAction()
}

// ─── Slot enum for weather widget (LEFT = temperature, RIGHT = icon) ──────────
enum class WeatherSlot { LEFT, RIGHT }

// ─── Apple HIG dimensions ─────────────────────────────────────────────────────
private val COMPACT_WIDTH     = 126.dp
private val COMPACT_HEIGHT    = 37.dp
private val COMPACT_RADIUS    = 18.5.dp

private const val EXPANDED_WIDTH_DP = 360
private val MEDIA_EXP_WIDTH   = EXPANDED_WIDTH_DP.dp
private val MEDIA_EXP_HEIGHT  = 170.dp
private val MEDIA_EXP_RADIUS  = 52.dp

private val NOTIFY_EXP_HEIGHT = 140.dp
private val CHARGE_EXP_HEIGHT = 84.dp
private val DEFAULT_EXP_WIDTH = EXPANDED_WIDTH_DP.dp
private val DEFAULT_EXP_RADIUS = 30.dp

// ─── Root composable ──────────────────────────────────────────────────────────

@Composable
fun DynamicIsland(
    state: IslandState,
    calibration: IslandCalibration,
    modifier: Modifier = Modifier,
    onMediaAction: (MediaAction) -> Unit = {},
    onCallAction: (CallAction) -> Unit = {}
) {
    val stateManager = remember { IslandStateManager.getInstance() }

    // Tap handler: toggle expand/collapse for interactive states
    val onTap: () -> Unit = remember(state) {
        {
            when {
                state.isExpanded -> stateManager.collapseCurrentState()
                state.allowInteraction -> stateManager.expandCurrentState(6000L)
                else -> { /* non-interactive states (Silent, Bluetooth) – no-op */ }
            }
        }
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        IslandShell(
            state       = state,
            calibration = calibration,
            onMediaAction = onMediaAction,
            onCallAction = onCallAction
        )
    }
}

// ─── Shell: picks dimensions and content per state ────────────────────────────

@Composable
private fun IslandShell(
    state: IslandState,
    calibration: IslandCalibration,
    onMediaAction: (MediaAction) -> Unit,
    onCallAction: (CallAction) -> Unit
) {
    val context = LocalContext.current
    val sizeManager = remember { getIslandSizeManager(context) }
    val overridesMap by sizeManager.overridesFlow.collectAsState()
    val expandedWidthScale by sizeManager.expandedWidthScaleFlow.collectAsState()
    val expandedHeightScale by sizeManager.expandedHeightScaleFlow.collectAsState()
    val override = overridesMap[state::class]

    // Apply user-calibrated overrides to the compact dimensions
    val calibW = calibration.pillWidth.dp
    val calibH = calibration.pillHeight.dp
    val calibR = calibration.cornerRadius.dp

    fun compactWidth(base: androidx.compose.ui.unit.Dp) = override?.width ?: base
    fun compactHeight(base: androidx.compose.ui.unit.Dp) = override?.height ?: base
    fun compactRadius(base: androidx.compose.ui.unit.Dp) = override?.cornerRadius ?: base
    
    fun scaleExpandedWidth(base: androidx.compose.ui.unit.Dp) = (base.value * expandedWidthScale).dp
    fun scaleExpandedHeight(base: androidx.compose.ui.unit.Dp) = (base.value * expandedHeightScale).dp
    // Radius generally scales with height or width; we'll pick average or just use height scale
    fun scaleExpandedRadius(base: androidx.compose.ui.unit.Dp) = (base.value * expandedHeightScale).dp

    when (state) {

        // ── Idle ──────────────────────────────────────────────────────────────
        is IslandState.Idle -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = compactWidth(calibW),
                targetHeight       = compactHeight(calibH),
                targetCornerRadius = compactRadius(calibR),
                rightContent = { IdleWidget() }
            )
        }

        // ── Weather (idle replacement) ────────────────────────────────────────
        is IslandState.Weather -> {
            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = scaleExpandedWidth(DEFAULT_EXP_WIDTH),
                    targetHeight       = scaleExpandedHeight(CHARGE_EXP_HEIGHT),
                    targetCornerRadius = scaleExpandedRadius(DEFAULT_EXP_RADIUS),
                    centerContent = {
                        WeatherWidget(
                            state      = state,
                            isExpanded = true,
                            slot       = WeatherSlot.LEFT
                        )
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = compactWidth(calibW),
                    targetHeight       = compactHeight(calibH),
                    targetCornerRadius = compactRadius(calibR),
                    leftContent = {
                        WeatherWidget(
                            state      = state,
                            isExpanded = false,
                            slot       = WeatherSlot.LEFT
                        )
                    },
                    rightContent = {
                        WeatherWidget(
                            state      = state,
                            isExpanded = false,
                            slot       = WeatherSlot.RIGHT
                        )
                    }
                )
            }
        }

        // ── Media ─────────────────────────────────────────────────────────────
        is IslandState.Media -> {
            val mediaCompactW = compactWidth(220.dp)
            val mediaCompactH = compactHeight(calibH)
            val mediaCompactR = compactRadius(calibR)

            val targetW = if (state.isExpanded) scaleExpandedWidth(MEDIA_EXP_WIDTH)  else mediaCompactW
            val targetH = if (state.isExpanded) scaleExpandedHeight(MEDIA_EXP_HEIGHT) else mediaCompactH
            val targetR = if (state.isExpanded) scaleExpandedRadius(MEDIA_EXP_RADIUS) else mediaCompactR

            if (state.isExpanded) {
                // Expanded: single full-width panel – no dead zone split needed
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    centerDeadZoneWidth = 0.dp,
                    centerContent = {
                        MediaWidget(
                            state         = state,
                            slot          = MediaSlot.LEFT,
                            isExpanded    = true,
                            onMediaAction = onMediaAction
                        )
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    leftContent = {
                        MediaWidget(state = state, slot = MediaSlot.LEFT, onMediaAction = onMediaAction)
                    },
                    rightContent = {
                        MediaWidget(state = state, slot = MediaSlot.RIGHT, onMediaAction = onMediaAction)
                    }
                )
            }
        }

        // ── Charging ──────────────────────────────────────────────────────────
        is IslandState.Charging -> {
            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = scaleExpandedWidth(DEFAULT_EXP_WIDTH),
                    targetHeight       = scaleExpandedHeight(CHARGE_EXP_HEIGHT),
                    targetCornerRadius = scaleExpandedRadius(DEFAULT_EXP_RADIUS),
                    centerContent = {
                        ChargingWidget(
                            state      = state,
                            slot       = ChargingSlot.LEFT,
                            isExpanded = true
                        )
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = compactWidth(calibW),
                    targetHeight       = compactHeight(calibH),
                    targetCornerRadius = compactRadius(calibR),
                    leftContent = {
                        ChargingWidget(
                            state      = state,
                            slot       = ChargingSlot.LEFT,
                            isExpanded = false
                        )
                    },
                    rightContent = {
                        ChargingWidget(state = state, slot = ChargingSlot.RIGHT, isExpanded = false)
                    }
                )
            }
        }

        // ── Notification ──────────────────────────────────────────────────────
        is IslandState.Notification -> {
            val notifCompactW = compactWidth(215.dp)
            val notifCompactH = compactHeight(calibH)
            val notifCompactR = compactRadius(calibR)

            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else notifCompactW
            val targetH = if (state.isExpanded) scaleExpandedHeight(notifCompactH) else notifCompactH // Maintain base height
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else notifCompactR

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth             = targetW,
                    targetHeight            = targetH,
                    targetCornerRadius      = targetR,
                    targetBottomPanelHeight = scaleExpandedHeight(NOTIFY_EXP_HEIGHT),
                    centerContent = {
                        NotificationWidget(state = state, slot = NotificationSlot.LEFT)
                    },
                    bottomContent = {
                        NotificationWidget(state = state, isExpanded = true)
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    leftContent  = { NotificationWidget(state = state, slot = NotificationSlot.LEFT) },
                    rightContent = { /* empty right for notifications */ }
                )
            }
        }

        // ── Bluetooth ─────────────────────────────────────────────────────────
        is IslandState.Bluetooth -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = compactWidth(220.dp),
                targetHeight       = compactHeight(calibH),
                targetCornerRadius = compactRadius(calibR),
                leftContent  = { BluetoothWidget(state = state, slot = BluetoothSlot.LEFT) },
                rightContent = { BluetoothWidget(state = state, slot = BluetoothSlot.RIGHT) }
            )
        }

        // ── Silent / DND ──────────────────────────────────────────────────────
        is IslandState.Silent -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = compactWidth(160.dp),
                targetHeight       = compactHeight(calibH),
                targetCornerRadius = compactRadius(calibR),
                leftContent  = { SilentWidget(state = state, slot = SilentSlot.LEFT) },
                rightContent = { SilentWidget(state = state, slot = SilentSlot.RIGHT) }
            )
        }

        // ── Volume ───────────────────────────────────────────────��────────────
        is IslandState.Volume -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = compactWidth(calibW),
                targetHeight       = compactHeight(calibH),
                targetCornerRadius = compactRadius(calibR),
                rightContent = { IdleWidget() }   // placeholder; add VolumeWidget if needed
            )
        }

        // ── Call ──────────────────────────────────────────────────────────────
        is IslandState.Call -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else compactWidth(calibW),
                targetHeight       = if (state.isExpanded) scaleExpandedHeight(120.dp) else compactHeight(calibH),
                targetCornerRadius = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else compactRadius(calibR),
                targetBottomPanelHeight = if (state.isExpanded) scaleExpandedHeight(72.dp) else 0.dp,
                leftContent  = { CallWidget(state = state, slot = CallSlot.LEFT) },
                rightContent = { CallWidget(state = state, slot = CallSlot.RIGHT) },
                bottomContent = {
                    if (state.isExpanded) {
                        CallWidget(state = state, slot = CallSlot.BOTTOM, onCallAction = onCallAction)
                    }
                }
            )
        }
    }
}
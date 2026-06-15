// File: app/src/main/java/com/miui/dynamicisland/ui/island/DynamicIsland.kt
// Purpose: Root composable for the island overlay.
//          Decides which widget to show, handles tap→expand/collapse.
// Hinglish: Yahan se poora island control hota hai – state ke hisab se widget dikhta hai
//           aur tap se expand/collapse hota hai.

package com.miui.dynamicisland.ui.island

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.font.FontWeight
import com.miui.dynamicisland.manager.IslandCalibration
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.manager.getIslandSizeManager
import com.miui.dynamicisland.ui.components.*
import com.miui.dynamicisland.ui.timer.*
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.ui.states.withExpanded

// ─── Sealed action type for media controls ────────────────────────────────────
sealed class MediaAction {
    object PlayPause : MediaAction()
    object Next      : MediaAction()
    object Previous  : MediaAction()
    data class Seek(val position: Float) : MediaAction()   // 0f–1f progress fraction
    object LaunchApp : MediaAction()
}

// ─── Sealed action type for call controls ─────────────────────────────────────
sealed class CallAction {
    object Accept  : CallAction()
    object Decline : CallAction()
    object End     : CallAction()
    object Mute    : CallAction()
    object ToggleSpeaker : CallAction()
    object LaunchApp     : CallAction()
}

// ─── Sealed action type for weather ──────────────────────────────────────────
sealed class WeatherAction {
    object Refresh : WeatherAction()
}

// ─── Sealed action type for clipboard ────────────────────────────────────────
sealed class ClipboardAction {
    object Search    : ClipboardAction()
    object Translate : ClipboardAction()
    object Share     : ClipboardAction()
}

// ─── Sealed action type for timer ───────────────────────────────────────────
sealed class TimerAction {
    object PauseResume : TimerAction()
    object Reset       : TimerAction()
    object Secondary   : TimerAction() // Lap or +1m
}

// ─── Slot enum for weather widget (LEFT = temperature, RIGHT = icon) ──────────
enum class WeatherSlot { LEFT, RIGHT }

// ─── Apple HIG dimensions ─────────────────────────────────────────────────────
private val COMPACT_WIDTH     = 126.dp
private val COMPACT_HEIGHT    = 37.dp
private val COMPACT_RADIUS    = 18.5.dp

private const val EXPANDED_WIDTH_DP = 360
private val MEDIA_EXP_WIDTH   = EXPANDED_WIDTH_DP.dp
private val MEDIA_EXP_HEIGHT  = 230.dp
private val MEDIA_EXP_RADIUS  = 52.dp

private val NOTIFY_EXP_HEIGHT = 140.dp
private val CHARGE_EXP_HEIGHT = 84.dp
private val WEATHER_EXP_HEIGHT = 300.dp
private val DEFAULT_EXP_WIDTH = EXPANDED_WIDTH_DP.dp
private val DEFAULT_EXP_RADIUS = 30.dp

// ─── Root composable ──────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DynamicIsland(
    state: IslandState,
    calibration: IslandCalibration,
    modifier: Modifier = Modifier,
    onMediaAction: (MediaAction) -> Unit = {},
    onCallAction: (CallAction) -> Unit = {},
    onWeatherAction: (WeatherAction) -> Unit = {},
    onClipboardAction: (ClipboardAction) -> Unit = {},
    onTimerAction: (TimerAction) -> Unit = {}
) {
    val stateManager = remember { IslandStateManager.getInstance() }
    val allStates by stateManager.allStates.collectAsState()
    val isLandscape by stateManager.isLandscapeMode.collectAsState()
    val isFixNotch by stateManager.isFixNotchMode.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Landscape filtering for Fix Notch mode
    if (isLandscape && isFixNotch) {
        val isEssential = state is IslandState.Navigation || 
                         state is IslandState.Call || 
                         state is IslandState.Timer ||
                         state is IslandState.Media ||
                         state is IslandState.Progress
        if (!isEssential && state !is IslandState.Idle) {
             // If not essential but something is active, we might want to hide it
             // but if nothing is active (Idle), we definitely don't want to block the shell
             return
        }
    }

    // Use rememberUpdatedState to keep the latest actions without restarting pointerInput
    val currentOnMediaAction by rememberUpdatedState(onMediaAction)
    val currentOnCallAction by rememberUpdatedState(onCallAction)
    val currentOnWeatherAction by rememberUpdatedState(onWeatherAction)
    val currentOnClipboardAction by rememberUpdatedState(onClipboardAction)
    val currentOnTimerAction by rememberUpdatedState(onTimerAction)

    // 1. Single Click: Toggle Expand/Collapse
    val onTap = {
        if (state.isExpanded) {
            stateManager.collapseCurrentState()
        } else if (state.allowInteraction) {
            stateManager.expandCurrentState(6000L)
            if (state is IslandState.Weather) {
                currentOnWeatherAction(WeatherAction.Refresh)
            }
        }
        Unit
    }

    // 2. Double Click: Open associated App
    val onDoubleTap = {
        stateManager.collapseCurrentState()
        when (state) {
            is IslandState.Media -> {
                currentOnMediaAction(MediaAction.LaunchApp)
            }
            is IslandState.Call -> {
                currentOnCallAction(CallAction.LaunchApp)
            }
            is IslandState.Weather -> {
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_APP_WEATHER)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = context.packageManager.getLaunchIntentForPackage("com.miui.weather2") 
                        ?: context.packageManager.getLaunchIntentForPackage("com.google.android.apps.messaging")
                    fallbackIntent?.let { context.startActivity(it) }
                }
            }
            is IslandState.Notification -> {
                try {
                    state.contentIntent?.send()
                } catch (e: Exception) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(state.packageName)
                    launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent?.let { context.startActivity(it) }
                }
            }
            is IslandState.Navigation -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(state.packageName)
                launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent?.let { context.startActivity(it) }
            }
            else -> {}
        }
    }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    Box(
        modifier = modifier
            .wrapContentSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onDoubleClick = { currentOnDoubleTap() },
                onClick = { currentOnTap() }
            )
            .then(
                if (state.isExpanded && allStates.size > 1) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 50) {
                                stateManager.previousState()
                                if (allStates.getOrNull(stateManager.currentIndex.value) is IslandState.Weather) {
                                    currentOnWeatherAction(WeatherAction.Refresh)
                                }
                            } else if (dragAmount < -50) {
                                stateManager.nextState()
                                if (allStates.getOrNull(stateManager.currentIndex.value) is IslandState.Weather) {
                                    currentOnWeatherAction(WeatherAction.Refresh)
                                }
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        IslandShell(
            state       = state,
            calibration = calibration,
            onMediaAction = currentOnMediaAction,
            onCallAction = currentOnCallAction,
            onWeatherAction = currentOnWeatherAction,
            onClipboardAction = currentOnClipboardAction,
            onTimerAction = currentOnTimerAction,
            queueCount   = allStates.size
        )
    }
}

@Composable
fun QueueIndicator(count: Int) {
    if (count <= 1) return
    Row(
        modifier = Modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count.coerceAtMost(3)) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

// ─── Shell: picks dimensions and content per state ────────────────────────────

@Composable
private fun IslandShell(
    state: IslandState,
    calibration: IslandCalibration,
    onMediaAction: (MediaAction) -> Unit,
    onCallAction: (CallAction) -> Unit,
    onWeatherAction: (WeatherAction) -> Unit,
    onClipboardAction: (ClipboardAction) -> Unit = {},
    onTimerAction: (TimerAction) -> Unit = {},
    queueCount: Int = 0
) {
    val context = LocalContext.current
    val sizeManager = remember { getIslandSizeManager(context) }
    val overridesMap by sizeManager.overridesFlow.collectAsState()
    val globalExpandedWidthScale by sizeManager.expandedWidthScaleFlow.collectAsState()
    val globalExpandedHeightScale by sizeManager.expandedHeightScaleFlow.collectAsState()
    val override = overridesMap[state::class]
    
    val expandedWidthScale = override?.expandedWidthScale ?: globalExpandedWidthScale
    val expandedHeightScale = override?.expandedHeightScale ?: globalExpandedHeightScale

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
                    targetHeight       = scaleExpandedHeight(WEATHER_EXP_HEIGHT),
                    targetCornerRadius = scaleExpandedRadius(DEFAULT_EXP_RADIUS),
                    horizontalPadding  = 0.dp, // Weather background should be edge-to-edge
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            WeatherWidget(
                                state      = state,
                                isExpanded = false,
                                slot       = WeatherSlot.RIGHT
                            )
                        }
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
                    horizontalPadding   = 0.dp, // Media background should be edge-to-edge
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            MediaWidget(state = state, slot = MediaSlot.RIGHT, onMediaAction = onMediaAction)
                        }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            ChargingWidget(state = state, slot = ChargingSlot.RIGHT, isExpanded = false)
                        }
                    }
                )
            }
        }

        // ── Notification ──────────────────────────────────────────────────────
        is IslandState.Notification -> {
            val hasMultiple = state.queueCount > 1
            val notifCompactW = if (hasMultiple) compactWidth(180.dp) else compactWidth(126.dp)
            val notifCompactH = compactHeight(calibH)
            val notifCompactR = compactRadius(calibR)

            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else notifCompactW
            val targetH = if (state.isExpanded) scaleExpandedHeight(190.dp) else notifCompactH
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else notifCompactR

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth             = targetW,
                    targetHeight            = targetH,
                    targetCornerRadius      = targetR,
                    targetBottomPanelHeight = 0.dp,
                    centerContent = {
                        NotificationWidget(state = state, isExpanded = true)
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    leftContent  = { NotificationWidget(state = state, slot = NotificationSlot.LEFT) },
                    rightContent = { QueueIndicator(queueCount) }
                )
            }
        }

        // ── Bluetooth ─────────────────────────────────────────────────────────
        is IslandState.Bluetooth -> {
            val btCompactW = compactWidth(220.dp)
            val btCompactH = compactHeight(calibH)
            val btCompactR = compactRadius(calibR)

            val btExpW = scaleExpandedWidth(320.dp)
            val btExpH = scaleExpandedHeight(300.dp)
            val btExpR = scaleExpandedRadius(30.dp)

            val targetW = if (state.isExpanded) btExpW else btCompactW
            val targetH = if (state.isExpanded) btExpH else btCompactH
            val targetR = if (state.isExpanded) btExpR else btCompactR

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    centerDeadZoneWidth = 0.dp,
                    horizontalPadding = 16.dp,
                    centerContent = { BluetoothExpandedWidget(state = state) }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    leftContent  = { BluetoothWidget(state = state, slot = BluetoothSlot.LEFT) },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            BluetoothWidget(state = state, slot = BluetoothSlot.RIGHT)
                        }
                    }
                )
            }
        }

        // ── Silent / DND ──────────────────────────────────────────────────────
        is IslandState.Silent -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = compactWidth(160.dp),
                targetHeight       = compactHeight(calibH),
                targetCornerRadius = compactRadius(calibR),
                leftContent  = { SilentWidget(state = state, slot = SilentSlot.LEFT) },
                rightContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QueueIndicator(queueCount)
                        SilentWidget(state = state, slot = SilentSlot.RIGHT)
                    }
                }
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
            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else compactWidth(calibW)
            val targetH = if (state.isExpanded) scaleExpandedHeight(82.dp) else compactHeight(calibH)
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else compactRadius(calibR)

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth             = targetW,
                    targetHeight            = targetH,
                    targetCornerRadius      = targetR,
                    targetBottomPanelHeight = 0.dp,
                    centerContent = {
                        CallWidget(state = state, isExpanded = true, onCallAction = onCallAction)
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    leftContent  = { CallWidget(state = state, slot = CallSlot.LEFT) },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            CallWidget(state = state, slot = CallSlot.RIGHT)
                        }
                    }
                )
            }
        }

        // ── Lock Screen ───────────────────────────────────────────────────────
        is IslandState.LockScreen -> {
            AnimatedCutoutSafeIslandShell(
                targetWidth        = compactWidth(calibW),
                targetHeight       = compactHeight(calibH),
                targetCornerRadius = compactRadius(calibR),
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.padding(start = 12.dp).size(20.dp),
                        tint = Color.White
                    )
                },
                rightContent = {
                    if (state.notificationCount > 0) {
                        Text(
                            text = state.notificationCount.toString(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            )
        }

        // ── Navigation ────────────────────────────────────────────────────────
        is IslandState.Navigation -> {
            val navCompactW = compactWidth(126.dp)
            val navCompactH = compactHeight(calibH)
            val navCompactR = compactRadius(calibR)

            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else navCompactW
            val targetH = if (state.isExpanded) scaleExpandedHeight(160.dp) else navCompactH
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else navCompactR

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    centerContent = {
                        NavigationWidget(state = state, slot = NavigationSlot.LEFT, isExpanded = true)
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth        = targetW,
                    targetHeight       = targetH,
                    targetCornerRadius = targetR,
                    leftContent  = { NavigationWidget(state = state, slot = NavigationSlot.LEFT) },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            NavigationWidget(state = state, slot = NavigationSlot.RIGHT)
                        }
                    }
                )
            }
        }

        // ── Progress ──────────────────────────────────────────────────────────
        is IslandState.Progress -> {
            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else compactWidth(200.dp)
            val targetH = if (state.isExpanded) scaleExpandedHeight(110.dp) else compactHeight(calibH)
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else compactRadius(calibR)

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    centerContent = {
                        ProgressWidget(state = state, slot = ProgressSlot.LEFT, isExpanded = true)
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    progress = state.progress,
                    progressColor = if (state.isDownload) Color(0xFF0A84FF).copy(alpha = 0.4f) else Color(0xFF30D158).copy(alpha = 0.4f),
                    leftContent = { ProgressWidget(state = state, slot = ProgressSlot.LEFT) },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            ProgressWidget(state = state, slot = ProgressSlot.RIGHT)
                        }
                    }
                )
            }
        }

        // ── Clipboard ─────────────────────────────────────────────────────────
        is IslandState.Clipboard -> {
            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else compactWidth(160.dp)
            val targetH = if (state.isExpanded) scaleExpandedHeight(140.dp) else compactHeight(calibH)
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else compactRadius(calibR)

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    centerContent = {
                        ClipboardWidget(state = state, slot = ClipboardSlot.LEFT, isExpanded = true, onAction = onClipboardAction)
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    leftContent = { ClipboardWidget(state = state, slot = ClipboardSlot.LEFT) },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            ClipboardWidget(state = state, slot = ClipboardSlot.RIGHT)
                        }
                    }
                )
            }
        }

        // ── Timer ─────────────────────────────────────────────────────────────
        is IslandState.Timer -> {
            val timerCompactW = compactWidth(140.dp)
            val timerCompactH = compactHeight(calibH)
            val timerCompactR = compactRadius(calibR)

            val targetW = if (state.isExpanded) scaleExpandedWidth(DEFAULT_EXP_WIDTH) else timerCompactW
            val targetH = if (state.isExpanded) scaleExpandedHeight(82.dp) else timerCompactH
            val targetR = if (state.isExpanded) scaleExpandedRadius(DEFAULT_EXP_RADIUS) else timerCompactR

            if (state.isExpanded) {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    centerContent = {
                        TimerPill(
                            state = state,
                            slot = TimerSlot.LEFT,
                            isExpanded = true,
                            onAction = onTimerAction
                        )
                    }
                )
            } else {
                AnimatedCutoutSafeIslandShell(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    targetCornerRadius = targetR,
                    leftContent = {
                        TimerPill(
                            state = state,
                            slot = TimerSlot.LEFT,
                            onAction = onTimerAction
                        )
                    },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QueueIndicator(queueCount)
                            TimerPill(
                                state = state,
                                slot = TimerSlot.RIGHT,
                                onAction = onTimerAction
                            )
                        }
                    }
                )
            }
        }
    }
}

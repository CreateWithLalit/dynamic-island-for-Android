// File: app/src/main/java/com/miui/dynamicisland/calibration/CalibrationActivity.kt
// Purpose: Allows user to drag island and adjust shape/dimensions in real time.
// Hinglish: Is activity mein user island ko drag kar sakta hai aur resize kar sakta hai.
//           Slider change hote hi overlay update ho jaata hai.
//
// FIX: Slider callbacks ab viewModel ke through CalibrationManager ko update karte hain,
//      isliye overlay bhi turant change ho jaata hai.

package com.miui.dynamicisland.calibration

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.manager.CalibrationManager
import com.miui.dynamicisland.service.IslandForegroundService
import com.miui.dynamicisland.ui.theme.DynamicIslandTheme
import com.miui.dynamicisland.util.IslandLogger
import com.miui.dynamicisland.util.PermissionUtils
import com.miui.dynamicisland.util.WindowUtils
import kotlin.math.roundToInt

class CalibrationActivity : ComponentActivity() {

    private val viewModel: CalibrationViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CalibrationViewModel(CalibrationManager(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicIslandTheme {
                Surface(color = Color.Black) {
                    CalibrationScreen(
                        viewModel = viewModel,
                        onSave = {
                            viewModel.saveCalibration()
                            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
                        },
                        onReset = {
                            viewModel.resetCalibration()
                            Toast.makeText(this, "Reset to Defaults", Toast.LENGTH_SHORT).show()
                        },
                        onStartService = { startIslandService() }
                    )
                }
            }
        }
    }

    private fun startIslandService() {
        if (!PermissionUtils.canDrawOverlays(this)) {
            startActivity(PermissionUtils.getOverlayPermissionIntentWithPackage(this))
            return
        }
        val intent = Intent(this, IslandForegroundService::class.java).apply {
            action = IslandForegroundService.ACTION_START
        }
        startForegroundService(intent)
        finish()
    }
}

@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onStartService: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val calibration by viewModel.calibration.collectAsState()
    val density = LocalDensity.current.density

    // Calculate real camera position for the red dot
    val screenWidth = WindowUtils.getScreenWidth(context)
    val cutoutRect = WindowUtils.getDisplayCutoutRect(context)
    
    val dotX: Float
    val dotY: Float
    
    if (cutoutRect != null && screenWidth > 0) {
        val centerX = screenWidth / 2f
        val cutoutCenterX = cutoutRect.centerX().toFloat()
        dotX = (cutoutCenterX - centerX) / density
        dotY = 8f // Use slightly offset Y within the preview
        IslandLogger.d("Calibration", "Hardware cutout detected: X=${cutoutRect.centerX()}, Y=${cutoutRect.centerY()}", null)
    } else {
        dotX = 0f
        dotY = 8f
        IslandLogger.d("Calibration", "No hardware cutout detected, using default center", null)
    }

    // Local state mirrors calibration – updated on every slider/drag event
    var offsetX      by remember { mutableFloatStateOf(0f) }
    var offsetY      by remember { mutableFloatStateOf(0f) }
    var cornerRadius by remember { mutableFloatStateOf(0f) }
    var pillWidth    by remember { mutableFloatStateOf(0f) }
    var pillHeight   by remember { mutableFloatStateOf(0f) }

    // Sync from DataStore on first load (and any external reset)
    LaunchedEffect(calibration) {
        offsetX      = calibration.offsetX
        offsetY      = calibration.offsetY
        cornerRadius = calibration.cornerRadius
        pillWidth    = calibration.pillWidth
        pillHeight   = calibration.pillHeight
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Island Calibration",
            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
        )
        Text(
            "Drag the island or use sliders to align with your camera.",
            color = Color.Gray, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── Draggable Preview ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Red dot = camera reference (positioned at real hardware cutout X offset)
            Box(
                Modifier
                    .size(12.dp)
                    .offset(x = dotX.dp, y = dotY.dp)
                    .background(Color.Red, CircleShape)
                    .align(Alignment.TopCenter)
            )

            // Draggable island preview
            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(width = pillWidth.dp, height = pillHeight.dp)
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x / density
                            offsetY += dragAmount.y / density
                            // Real-time update → propagates to CalibrationManager → overlay moves
                            viewModel.updateOffsets(offsetX, offsetY)
                        }
                    }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Sliders ───────────────────────────────────────────────────────────

        CalibrationSlider("Y Offset (Position)", offsetY, -50f..150f) { newVal ->
            offsetY = newVal
            viewModel.updateOffsets(offsetX, offsetY)   // real-time overlay update
        }
        CalibrationSlider("X Offset", offsetX, -150f..150f) { newVal ->
            offsetX = newVal
            viewModel.updateOffsets(offsetX, offsetY)
        }
        CalibrationSlider("Corner Radius", cornerRadius, 0f..40f) { newVal ->
            cornerRadius = newVal
            viewModel.updateCornerRadius(newVal)         // real-time update
        }
        CalibrationSlider("Pill Width", pillWidth, 80f..350f) { newVal ->
            pillWidth = newVal
            viewModel.updatePillWidth(newVal)            // real-time update
        }
        CalibrationSlider("Pill Height", pillHeight, 20f..80f) { newVal ->
            pillHeight = newVal
            viewModel.updatePillHeight(newVal)           // real-time update
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
        ) {
            Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Reset to Defaults", color = Color.White)
        }

        Button(
            onClick = onStartService,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Apply & Start Island")
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun CalibrationSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text("${value.roundToInt()}", color = Color.Gray, fontSize = 14.sp)
        }
        Slider(
            value          = value,
            onValueChange  = onValueChange,
            valueRange     = range,
            colors         = SliderDefaults.colors(
                thumbColor       = Color.White,
                activeTrackColor = Color.White
            )
        )
    }
}
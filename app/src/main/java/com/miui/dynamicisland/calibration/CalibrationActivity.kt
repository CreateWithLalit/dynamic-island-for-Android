// File: app/src/main/java/com/miui/dynamicisland/calibration/CalibrationActivity.kt
package com.miui.dynamicisland.calibration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.manager.CalibrationManager
import com.miui.dynamicisland.service.IslandForegroundService
import com.miui.dynamicisland.ui.theme.DynamicIslandTheme
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
    val context = LocalContext.current
    val calibration by viewModel.calibration.collectAsState()
    val density = LocalDensity.current.density

    // Contact Permission State
    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Contacts Permission Granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val screenWidth = WindowUtils.getScreenWidth(context)
    val cutoutRect = WindowUtils.getDisplayCutoutRect(context)
    val statusBarHeight = WindowUtils.getStatusBarHeight(context)

    val dotX: Float
    val dotY: Float

    if (cutoutRect != null && screenWidth > 0) {
        val centerX = screenWidth / 2f
        val cutoutCenterX = cutoutRect.centerX().toFloat()
        val cutoutCenterY = cutoutRect.centerY().toFloat()
        dotX = (cutoutCenterX - centerX) / density
        dotY = (cutoutCenterY - statusBarHeight) / density
    } else {
        dotX = 0f
        dotY = 8f
    }

    var offsetX      by remember { mutableFloatStateOf(0f) }
    var offsetY      by remember { mutableFloatStateOf(0f) }
    var cornerRadius by remember { mutableFloatStateOf(0f) }
    var pillWidth    by remember { mutableFloatStateOf(0f) }
    var pillHeight   by remember { mutableFloatStateOf(0f) }

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

        // ── Contact Permission Toggle ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Contacts, null, tint = if (hasContactPermission) Color(0xFF30D158) else Color.Gray)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Caller ID Identification", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        if (hasContactPermission) "Permission Granted" else "Required to show caller names",
                        color = if (hasContactPermission) Color(0xFF30D158) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = hasContactPermission,
                    onCheckedChange = { 
                        if (it) {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            Toast.makeText(context, "Please disable in System Settings", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF30D158))
                )
            }
        }

        // ── Draggable Preview ─────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            val dotSize = 12.dp
            val maxX = ((maxWidth - pillWidth.dp).coerceAtLeast(0.dp) / 2)
            val maxY = (maxHeight - pillHeight.dp).coerceAtLeast(0.dp)
            val maxDotX = ((maxWidth - dotSize).coerceAtLeast(0.dp) / 2)
            val maxDotY = (maxHeight - dotSize).coerceAtLeast(0.dp)
            val clampedDotX = dotX.dp.coerceIn(-maxDotX, maxDotX)
            val clampedDotY = dotY.dp.coerceIn(0.dp, maxDotY)

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier
                        .size(dotSize)
                        .offset(x = clampedDotX, y = clampedDotY)
                        .background(Color.Red, CircleShape)
                        .align(Alignment.TopCenter)
                )

                Box(
                    modifier = Modifier
                        .offset(
                            x = offsetX.dp.coerceIn(-maxX, maxX),
                            y = offsetY.dp.coerceIn(0.dp, maxY)
                        )
                        .size(width = pillWidth.dp, height = pillHeight.dp)
                        .clip(RoundedCornerShape(cornerRadius.dp))
                        .background(Color.Black)
                        .pointerInput(maxX, maxY, density) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = offsetX + (dragAmount.x / density)
                                val newY = offsetY + (dragAmount.y / density)
                                offsetX = newX.coerceIn(-maxX.value, maxX.value)
                                offsetY = newY.coerceIn(0f, maxY.value)
                                viewModel.updateOffsets(offsetX, offsetY)
                            }
                        }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Sliders ───────────────────────────────────────────────────────────
        CalibrationSlider("Y Offset (Position)", offsetY, -50f..150f) { newVal ->
            offsetY = newVal
            viewModel.updateOffsets(offsetX, offsetY)
        }
        CalibrationSlider("X Offset", offsetX, -150f..150f) { newVal ->
            offsetX = newVal
            viewModel.updateOffsets(offsetX, offsetY)
        }
        CalibrationSlider("Corner Radius", cornerRadius, 0f..40f) { newVal ->
            cornerRadius = newVal
            viewModel.updateCornerRadius(newVal)
        }
        CalibrationSlider("Pill Width", pillWidth, 80f..350f) { newVal ->
            pillWidth = newVal
            viewModel.updatePillWidth(newVal)
        }
        CalibrationSlider("Pill Height", pillHeight, 20f..80f) { newVal ->
            pillHeight = newVal
            viewModel.updatePillHeight(newVal)
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
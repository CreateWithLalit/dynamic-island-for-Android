// File: app/src/main/java/com/miui/dynamicisland/MainActivity.kt
// Purpose: Main UI for permission diagnosis and calibration access
// Hinglish: Is file mein app ka main dashboard hai jahan se permissions aur settings handle hoti hain.

package com.miui.dynamicisland

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.calibration.CalibrationActivity
import com.miui.dynamicisland.settings.IslandSizeSettingsActivity
import com.miui.dynamicisland.service.BluetoothIslandService
import com.miui.dynamicisland.service.IslandForegroundService
import com.miui.dynamicisland.ui.theme.DynamicIslandTheme
import com.miui.dynamicisland.util.MIUIUtils
import com.miui.dynamicisland.util.PermissionUtils
import com.miui.dynamicisland.util.OverlaySettings

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicIslandTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    PermissionDiagnosticScreen(
                        onCalibrationClick = { startActivity(Intent(this, CalibrationActivity::class.java)) },
                        onStartService = { startIslandService() },
                        onStopService = { stopIslandService() },
                        onStartBluetoothIsland = { startBluetoothIslandService() },
                        onStopBluetoothIsland = { stopBluetoothIslandService() }
                    )
                }
            }
        }
    }

    private fun startIslandService() {
        val intent = Intent(this, IslandForegroundService::class.java).apply {
            action = IslandForegroundService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopIslandService() {
        val intent = Intent(this, IslandForegroundService::class.java).apply {
            action = IslandForegroundService.ACTION_STOP
        }
        startForegroundService(intent)
    }

    private fun startBluetoothIslandService() {
        val intent = Intent(this, BluetoothIslandService::class.java).apply {
            action = BluetoothIslandService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopBluetoothIslandService() {
        val intent = Intent(this, BluetoothIslandService::class.java).apply {
            action = BluetoothIslandService.ACTION_STOP
        }
        startForegroundService(intent)
    }
}

@Composable
fun PermissionDiagnosticScreen(
    onCalibrationClick: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onStartBluetoothIsland: () -> Unit,
    onStopBluetoothIsland: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasOverlay by remember { mutableStateOf(PermissionUtils.canDrawOverlays(context)) }
    var hasNotification by remember { mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var hasPhoneState by remember { mutableStateOf(PermissionUtils.hasPhoneStatePermission(context)) }
    var hasAnswerCalls by remember { mutableStateOf(PermissionUtils.hasAnswerCallsPermission(context)) }
    var hasBluetoothConnect by remember { mutableStateOf(PermissionUtils.hasBluetoothConnectPermission(context)) }
    var hasContacts by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    var isDefaultDialer by remember { mutableStateOf(false) }
    var useAccessibilityOverlay by remember { mutableStateOf(OverlaySettings.isAccessibilityOverlayEnabled(context)) }
    var allowLockScreenOverlay by remember { mutableStateOf(OverlaySettings.isLockScreenOverlayEnabled(context)) }

    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(android.app.role.RoleManager::class.java)
    } else null

    val telecomManager = remember { context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager }

    val dialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isDefaultDialer = roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) == true
        } else {
            isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName
        }
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhoneState = granted
    }

    val answerCallsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAnswerCalls = granted
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothConnect = granted
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContacts = granted
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Auto refresh on resume would be better, but for now a simple check
    LaunchedEffect(Unit) {
        while(true) {
            hasOverlay = PermissionUtils.canDrawOverlays(context)
            hasNotification = PermissionUtils.isNotificationListenerEnabled(context)
            hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
            hasPhoneState = PermissionUtils.hasPhoneStatePermission(context)
            hasAnswerCalls = PermissionUtils.hasAnswerCallsPermission(context)
            hasBluetoothConnect = PermissionUtils.hasBluetoothConnectPermission(context)
            hasContacts = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            isDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) == true
            } else {
                telecomManager?.defaultDialerPackage == context.packageName
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dynamic Island", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Permission Diagnosis", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(32.dp))

        PermissionCard(
            title = "Overlay Permission",
            description = "Needed to show the island over other apps.",
            isGranted = hasOverlay,
            icon = Icons.Default.Layers,
            onClick = { context.startActivity(PermissionUtils.getOverlayPermissionIntentWithPackage(context)) }
        )

        PermissionCard(
            title = "Notification Access",
            description = "Needed to show music and notifications.",
            isGranted = hasNotification,
            icon = Icons.Default.Notifications,
            onClick = { context.startActivity(PermissionUtils.getNotificationListenerIntent(context)) }
        )

        PermissionCard(
            title = "Accessibility Access",
            description = "Needed for system events and backup notification detection.",
            isGranted = hasAccessibility,
            icon = Icons.Default.AccessibilityNew,
            onClick = { context.startActivity(PermissionUtils.getAccessibilityServiceIntent(context)) }
        )

        SettingToggleCard(
            title = "Accessibility Overlay Mode",
            description = "Uses AccessibilityService overlay to attempt lock screen/AOD visibility.",
            isEnabled = hasAccessibility,
            isChecked = useAccessibilityOverlay,
            onCheckedChange = { enabled ->
                useAccessibilityOverlay = enabled
                OverlaySettings.setAccessibilityOverlayEnabled(context, enabled)
            }
        )

        SettingToggleCard(
            title = "Show On Lock Screen (Best Effort)",
            description = "Adds lock screen window flags to app overlays.",
            isEnabled = true,
            isChecked = allowLockScreenOverlay,
            onCheckedChange = { enabled ->
                allowLockScreenOverlay = enabled
                OverlaySettings.setLockScreenOverlayEnabled(context, enabled)
            }
        )

        PermissionCard(
            title = "Phone State",
            description = "Needed to detect incoming and ongoing calls.",
            isGranted = hasPhoneState,
            icon = Icons.Default.Phone,
            onClick = { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
        )

        PermissionCard(
            title = "Answer Calls",
            description = "Needed for accept/decline actions on supported devices.",
            isGranted = hasAnswerCalls,
            icon = Icons.Default.Call,
            onClick = { answerCallsPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS) }
        )

        PermissionCard(
            title = "Contacts (Caller ID)",
            description = "Needed to show real names and photos of callers.",
            isGranted = hasContacts,
            icon = Icons.Default.Contacts,
            onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
        )

        PermissionCard(
            title = "Default Dialer",
            description = "Set as default phone app for full island integration.",
            isGranted = isDefaultDialer,
            icon = Icons.Default.Dialpad,
            onClick = {
                // Request CALL_PHONE permission first
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intent = roleManager?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                    if (intent != null) dialerLauncher.launch(intent)
                } else {
                    val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                        putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                    }
                    dialerLauncher.launch(intent)
                }
            }
        )

        PermissionCard(
            title = "Bluetooth Connect",
            description = "Needed to read Bluetooth device battery.",
            isGranted = hasBluetoothConnect,
            icon = Icons.Default.Bluetooth,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        )

        if (MIUIUtils.isMIUI()) {
            PermissionCard(
                title = "MIUI AutoStart",
                description = "Recommended for the service to keep running.",
                isGranted = false, // We can't easily check this
                icon = Icons.Default.SettingsSuggest,
                onClick = { try { context.startActivity(MIUIUtils.getAutoStartIntent()) } catch(e: Exception) { PermissionUtils.openAppDetails(context) } }
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onCalibrationClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Icon(Icons.Default.Tune, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("Island Calibration", color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { context.startActivity(Intent(context, IslandSizeSettingsActivity::class.java)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Icon(Icons.Default.Tune, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("Island Size Settings", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onStartService,
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = hasOverlay && hasNotification,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
            ) {
                Text("Start Service", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStopService,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
            ) {
                Text("Stop Service", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onStartBluetoothIsland,
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = hasOverlay && hasBluetoothConnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
            ) {
                Text("Start Bluetooth Island", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStopBluetoothIsland,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
            ) {
                Text("Stop Bluetooth Island", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, color = Color.Gray, fontSize = 12.sp)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = { if (isEnabled) onCheckedChange(it) },
                enabled = isEnabled
            )
        }
    }
}

@Composable
fun PermissionCard(title: String, description: String, isGranted: Boolean, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = if (isGranted) Color(0xFF30D158) else Color.Gray)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, color = Color.Gray, fontSize = 12.sp)
            }
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF30D158))
            } else {
                Icon(Icons.Default.ArrowForwardIos, null, Modifier.size(16.dp), tint = Color.Gray)
            }

        }
    }
}

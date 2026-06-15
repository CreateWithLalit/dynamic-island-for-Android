package com.miui.dynamicisland.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.manager.getIslandSizeManager
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.ui.theme.DynamicIslandTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.reflect.KClass

class IslandSizeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicIslandTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    IslandSizeSettingsScreen(
                        onDone = { finish() }
                    )
                }
            }
        }
    }
}

private data class StateSizingItem(
    val title: String,
    val subtitle: String,
    val stateClass: KClass<out IslandState>
)

@Composable
private fun IslandSizeSettingsScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val sizeManager = remember { getIslandSizeManager(context) }
    val overrides by sizeManager.overridesFlow.collectAsState()
    val expandedWidthScale by sizeManager.expandedWidthScaleFlow.collectAsState()
    val expandedHeightScale by sizeManager.expandedHeightScaleFlow.collectAsState()
    val scope = rememberCoroutineScope()

    val items = remember {
        listOf(
            StateSizingItem(
                title = "Music (Media)",
                subtitle = "Compact pill size for the music island",
                stateClass = IslandState.Media::class
            ),
            StateSizingItem(
                title = "Call",
                subtitle = "Compact pill size for incoming/ongoing calls",
                stateClass = IslandState.Call::class
            ),
            StateSizingItem(
                title = "Battery (Charging)",
                subtitle = "Compact pill size while charging",
                stateClass = IslandState.Charging::class
            ),
            StateSizingItem(
                title = "Notification",
                subtitle = "Compact pill size for notifications",
                stateClass = IslandState.Notification::class
            ),
            StateSizingItem(
                title = "Weather",
                subtitle = "Compact pill size for weather",
                stateClass = IslandState.Weather::class
            ),
            StateSizingItem(
                title = "Bluetooth",
                subtitle = "Compact pill size for bluetooth",
                stateClass = IslandState.Bluetooth::class
            ),
            StateSizingItem(
                title = "Silent/DND",
                subtitle = "Compact pill size for silent mode",
                stateClass = IslandState.Silent::class
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Island Size Settings",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Adjust compact pill width/height/radius per feature. Changes apply live if the overlay is running.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Expanded size", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Scale expanded cards width and height separately.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                ScaleSlider(
                    label = "Expanded width scale",
                    value = expandedWidthScale,
                    range = 0.7f..4.0f,
                    onValueChange = { newValue ->
                        scope.launch { sizeManager.updateExpandedWidthScale(newValue) }
                    }
                )
                ScaleSlider(
                    label = "Expanded height scale",
                    value = expandedHeightScale,
                    range = 0.7f..1.6f,
                    onValueChange = { newValue ->
                        scope.launch { sizeManager.updateExpandedHeightScale(newValue) }
                    }
                )
                Text(
                    text = "Tip: 1.0x keeps the original size.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        items.forEach { item ->
            val currentOverride = overrides[item.stateClass]
            val defaults = remember(item.stateClass) { sizeManager.getDefaultDimensionsForState(item.stateClass) }

            val widthDp = currentOverride?.width ?: defaults.width
            val heightDp = currentOverride?.height ?: defaults.height
            val radiusDp = currentOverride?.cornerRadius ?: defaults.cornerRadius
            val expWidthScale = currentOverride?.expandedWidthScale ?: expandedWidthScale
            val expHeightScale = currentOverride?.expandedHeightScale ?: expandedHeightScale

            var isExpandedSizing by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpandedSizing = !isExpandedSizing },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(item.subtitle, color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = if (isExpandedSizing) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    if (isExpandedSizing) {
                        Spacer(Modifier.height(16.dp))
                        
                        Text("Compact Pill Dimensions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))

                        SizeSlider(
                            label = "Width (dp)",
                            value = widthDp.value,
                            range = 80f..360f,
                            onValueChange = { newValue ->
                                scope.launch { sizeManager.updateDimensions(item.stateClass, width = newValue) }
                            }
                        )

                        SizeSlider(
                            label = "Height (dp)",
                            value = heightDp.value,
                            range = 20f..120f,
                            onValueChange = { newValue ->
                                scope.launch { sizeManager.updateDimensions(item.stateClass, height = newValue) }
                            }
                        )

                        SizeSlider(
                            label = "Corner radius (dp)",
                            value = radiusDp.value,
                            range = 0f..60f,
                            onValueChange = { newValue ->
                                scope.launch { sizeManager.updateDimensions(item.stateClass, cornerRadius = newValue) }
                            }
                        )

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                        Spacer(Modifier.height(16.dp))

                        Text("Expanded Card Dimensions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))

                        ScaleSlider(
                            label = "Expanded width scale",
                            value = expWidthScale,
                            range = 0.7f..4.0f,
                            onValueChange = { newValue ->
                                scope.launch { sizeManager.updateDimensions(item.stateClass, expandedWidthScale = newValue) }
                            }
                        )

                        ScaleSlider(
                            label = "Expanded height scale",
                            value = expHeightScale,
                            range = 0.8f..2.0f,
                            onValueChange = { newValue ->
                                scope.launch { sizeManager.updateDimensions(item.stateClass, expandedHeightScale = newValue) }
                            }
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { sizeManager.clearOverridesForState(item.stateClass) }
                                }
                            ) {
                                Text("Reset Feature", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFF2C2C2E))
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch { sizeManager.resetAllOverrides() }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Text("Reset All", color = Color.White)
            }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Tip: Keep height close to 37dp for best Apple-like look.",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SizeSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val step = 1f
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text("${value.roundToInt()}", color = Color.Gray, fontSize = 13.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
            }
            
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )

            IconButton(
                onClick = { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ScaleSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val step = 0.05f
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text("${"%.2f".format(value)}x", color = Color.Gray, fontSize = 13.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
            }

            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )

            IconButton(
                onClick = { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
            }
        }
    }
}

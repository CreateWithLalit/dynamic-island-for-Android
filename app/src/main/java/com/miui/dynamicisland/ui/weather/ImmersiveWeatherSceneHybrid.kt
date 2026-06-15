package com.miui.dynamicisland.ui.weather

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.miui.dynamicisland.R
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalTime
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewWeatherScene() {
    val sampleData = WeatherSceneData(
        condition = WeatherCondition.Clear,
        sunrise = LocalTime.of(6, 0),
        sunset = LocalTime.of(18, 30),
        currentTemp = 28,
        location = "Noida",
        hourlyForecast = listOf(
            HourlyForecast("9 am", 28, 0),
            HourlyForecast("12 pm", 31, 0),
            HourlyForecast("3 pm", 33, 0),
            HourlyForecast("6 pm", 32, 0),
            HourlyForecast("9 pm", 29, 0),
            HourlyForecast("12 am", 27, 0),
            HourlyForecast("3 am", 26, 0)
        )
    )
    
    Box(modifier = Modifier.size(width = 400.dp, height = 240.dp)) {
        ImmersiveWeatherSceneHybrid(
            weatherData = sampleData,
            currentTime = LocalTime.of(9, 30)
        )
    }
}

@Composable
fun ImmersiveWeatherSceneHybrid(
    weatherData: WeatherSceneData,
    currentTime: LocalTime,
    modifier: Modifier = Modifier
) {
    val timeOfDay = TimeOfDay.fromHour(currentTime.hour)
    val isNight = timeOfDay == TimeOfDay.Night
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(44.dp))
            .fillMaxSize()
    ) {
        // LAYER 0: Sky texture (photorealistic base)
        AnimatedSkyBackground(
            currentTime = currentTime,
            modifier = Modifier.fillMaxSize()
        )
        
        // LAYER 1: Volumetric clouds (shader or sprite)
        if (weatherData.condition != WeatherCondition.Clear) {
            VolumetricCloudOverlay(
                timeOfDay = timeOfDay,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = this.constraints.maxWidth.toFloat()
            val heightPx = this.constraints.maxHeight.toFloat()
            
            // LAYER 2: Celestial body
            val celestialPos = calculateCelestialPosition(
                currentTime = currentTime,
                sunrise = weatherData.sunrise,
                sunset = weatherData.sunset,
                containerSize = Size(widthPx, heightPx)
            )
            
            if (isNight) {
                RealisticMoon(
                    position = celestialPos,
                    phase = weatherData.moonPhase,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RealisticSun(
                    position = celestialPos,
                    intensity = if (timeOfDay == TimeOfDay.Noon) 1f else 0.7f,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // LAYER 3: Weather effects
        when (weatherData.condition) {
            WeatherCondition.Rain -> PremiumRainEffect(
                intensity = weatherData.precipitationIntensity,
                modifier = Modifier.fillMaxSize()
            )
            WeatherCondition.Storm -> {
                PremiumRainEffect(intensity = 1f, modifier = Modifier.fillMaxSize())
                LightningFlashEffect(active = true, modifier = Modifier.fillMaxSize())
            }
            WeatherCondition.Snow -> SnowEffectHybrid(
                intensity = weatherData.precipitationIntensity,
                modifier = Modifier.fillMaxSize()
            )
            else -> {}
        }
        
        // LAYER 4: Post-processing
        AtmosphericPostProcess(
            timeOfDay = timeOfDay,
            weatherCondition = weatherData.condition,
            modifier = Modifier.fillMaxSize()
        )
        
        // LAYER 5: Glassmorphism forecast panel
        GlassmorphismForecastPanel(
            hourlyData = weatherData.hourlyForecast,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // LAYER 6: Top info (time, location)
        WeatherHeader(
            location = weatherData.location,
            currentTime = currentTime,
            currentTemp = weatherData.currentTemp,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun AnimatedSkyBackground(
    currentTime: LocalTime,
    modifier: Modifier = Modifier
) {
    val timeOfDay = TimeOfDay.fromHour(currentTime.hour)
    val nextTimeOfDay = TimeOfDay.fromHour((currentTime.hour + 1) % 24)
    val progress = currentTime.minute / 60f
    
    val currentRes = getSkyResource(timeOfDay)
    val nextRes = getSkyResource(nextTimeOfDay)
    
    val infiniteTransition = rememberInfiniteTransition(label = "skyPan")
    val panOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pan"
    )
    
    val currentPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(currentRes)
            .crossfade(true)
            .build()
    )
    val nextPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(nextRes)
            .crossfade(true)
            .build()
    )
    
    Box(modifier = modifier) {
        Image(
            painter = currentPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -panOffset * 100f
                }
        )
        
        Image(
            painter = nextPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = progress
                    translationX = -panOffset * 100f
                }
        )
    }
}

fun getSkyResource(timeOfDay: TimeOfDay): Int = when(timeOfDay) {
    TimeOfDay.Dawn -> R.drawable.ic_launcher_background // TODO: Replace with sky_dawn
    TimeOfDay.Morning -> R.drawable.ic_launcher_background // TODO: Replace with sky_morning
    TimeOfDay.Noon -> R.drawable.ic_launcher_background // TODO: Replace with sky_noon
    TimeOfDay.GoldenHour -> R.drawable.ic_launcher_background // TODO: Replace with sky_golden
    TimeOfDay.Dusk -> R.drawable.ic_launcher_background // TODO: Replace with sky_dusk
    TimeOfDay.Night -> R.drawable.ic_launcher_background // TODO: Replace with sky_night
}

@Composable
fun VolumetricCloudOverlay(
    timeOfDay: TimeOfDay,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    
    val cloudSpeeds = listOf(0.1f, 0.25f, 0.5f)
    val cloudOpacities = listOf(0.3f, 0.5f, 0.7f)
    val cloudScales = listOf(1.5f, 1.0f, 0.7f)
    
    val noiseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "noise"
    )
    
    Canvas(modifier = modifier) {
        cloudSpeeds.forEachIndexed { index, speed ->
            val layerOffset = noiseOffset * speed
            // Simplified drawing for now since we don't have a Perlin noise implementation here
            // In a real app, you'd use a Shader or a noise texture.
            drawCircle(
                color = getCloudColor(timeOfDay).copy(alpha = cloudOpacities[index]),
                radius = size.width * cloudScales[index] * 0.2f,
                center = Offset(
                    (layerOffset % size.width),
                    size.height * (0.2f + index * 0.1f)
                )
            )
        }
    }
}

fun getCloudColor(timeOfDay: TimeOfDay): Color = when(timeOfDay) {
    TimeOfDay.Dawn -> Color(0xFFF0E68C)
    TimeOfDay.Morning, TimeOfDay.Noon -> Color.White
    TimeOfDay.GoldenHour -> Color(0xFFFFDAB9)
    TimeOfDay.Dusk -> Color(0xFFFF7F50)
    TimeOfDay.Night -> Color(0xFF708090)
}

@Composable
fun RealisticSun(
    position: Offset,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background), // TODO: Replace with sun_body
            contentDescription = null,
            modifier = Modifier
                .size(80.dp * intensity * sunGlow)
                .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect.createBlurEffect(
                            20f * intensity,
                            20f * intensity,
                            Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
        )
        
        Image(
            painter = painterResource(R.drawable.ic_launcher_background), // TODO: Replace with sun_rays
            contentDescription = null,
            modifier = Modifier
                .size(200.dp * intensity)
                .offset { IntOffset(position.x.toInt() - 60.dp.toPx().toInt(), position.y.toInt() - 60.dp.toPx().toInt()) }
                .graphicsLayer {
                    rotationZ = (System.currentTimeMillis() % 60000) / 60000f * 360f
                    alpha = 0.6f * intensity
                }
        )
        
        if (intensity > 0.8f) {
            LensFlareElements(position = position)
        }
    }
}

@Composable
fun LensFlareElements(position: Offset) {
    val flarePositions = listOf(
        Offset(-0.5f, 0.3f), Offset(0.3f, -0.2f), Offset(-0.2f, -0.4f)
    )
    
    flarePositions.forEach { offset ->
        Image(
            painter = painterResource(R.drawable.ic_launcher_background), // TODO: Replace with lens_flare_hex
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .offset {
                    IntOffset(
                        (position.x + offset.x * 100).toInt(),
                        (position.y + offset.y * 100).toInt()
                    )
                }
                .graphicsLayer {
                    alpha = 0.3f
                }
        )
    }
}

@Composable
fun RealisticMoon(
    position: Offset,
    phase: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background), // TODO: Replace with moon_texture
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
                .graphicsLayer {
                    alpha = 0.9f
                }
        )
        
        Canvas(
            modifier = Modifier
                .size(60.dp)
                .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
        ) {
            val shadowWidth = size.width * (1f - abs(phase - 0.5f) * 2f)
            drawRect(
                color = Color(0xFF0B1026).copy(alpha = 0.85f),
                size = Size(shadowWidth, size.height),
                topLeft = if (phase < 0.5f) Offset(0f, 0f) 
                         else Offset(size.width - shadowWidth, 0f)
            )
        }
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset { IntOffset(position.x.toInt() - 20, position.y.toInt() - 20) }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun PremiumRainEffect(
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )
    
    Canvas(modifier = modifier) {
        repeat((150 * intensity).toInt()) { i ->
            val random = Random(i)
            val x = random.nextFloat() * size.width
            val y = (random.nextFloat() * size.height + time * 800f) % size.height
            val length = 40f + random.nextFloat() * 30f
            
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFB0C4DE).copy(alpha = 0.0f),
                        Color(0xFFB0C4DE).copy(alpha = 0.6f * intensity),
                        Color(0xFFB0C4DE).copy(alpha = 0.0f)
                    ),
                    startY = y - length,
                    endY = y
                ),
                start = Offset(x, y - length),
                end = Offset(x, y),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
fun LightningFlashEffect(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(active) {
        while (active) {
            delay((3000 + Random.nextInt(6000)).toLong())
            
            repeat(3) {
                flashAlpha = 0.9f
                delay(50)
                flashAlpha = 0.2f
                delay(50)
            }
            flashAlpha = 0f
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = flashAlpha,
        animationSpec = snap(),
        label = "lightning"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = alpha))
    )
    
    if (alpha > 0.5f) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background), // TODO: Replace with lightning_bolt
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alpha = alpha
        )
    }
}

@Composable
fun SnowEffectHybrid(
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow"
    )

    Canvas(modifier = modifier) {
        repeat((100 * intensity).toInt()) { i ->
            val random = Random(i)
            val x = (random.nextFloat() * size.width + time * 100f) % size.width
            val y = (random.nextFloat() * size.height + time * 200f) % size.height
            val radius = 2f + random.nextFloat() * 3f
            
            drawCircle(
                color = Color.White.copy(alpha = 0.8f * intensity),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun GlassmorphismForecastPanel(
    hourlyData: List<HourlyForecast>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect
                        .createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            hourlyData.take(7).forEach { hour ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = hour.time,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "${hour.temp}°",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AtmosphericPostProcess(
    timeOfDay: TimeOfDay,
    weatherCondition: WeatherCondition,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background), // TODO: Replace with vignette_overlay
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        
        val tintColor = when(timeOfDay) {
            TimeOfDay.Dawn -> Color(0xFFFFA07A).copy(alpha = 0.1f)
            TimeOfDay.GoldenHour -> Color(0xFFFFD700).copy(alpha = 0.05f)
            TimeOfDay.Dusk -> Color(0xFF8B4513).copy(alpha = 0.15f)
            TimeOfDay.Night -> Color(0xFF191970).copy(alpha = 0.2f)
            else -> Color.Transparent
        }
        
        Box(modifier = Modifier.fillMaxSize().background(tintColor))
        
        if (weatherCondition in listOf(WeatherCondition.Rain, WeatherCondition.Fog)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.1f),
                                Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun WeatherHeader(
    location: String,
    currentTime: LocalTime,
    currentTemp: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tuesday, 2 Jun", // Hardcoded for matching reference, should be dynamic
                    color = Color.White,
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = "${currentTemp}°",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val hour = if (currentTime.hour % 12 == 0) 12 else currentTime.hour % 12
        val amPm = if (currentTime.hour < 12) "am" else "pm"
        Text(
            text = String.format(java.util.Locale.getDefault(), "%d:%02d %s", hour, currentTime.minute, amPm),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light
        )
    }
}

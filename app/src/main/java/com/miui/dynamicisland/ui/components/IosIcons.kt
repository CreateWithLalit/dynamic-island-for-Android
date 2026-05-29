// File: app/src/main/java/com/miui/dynamicisland/ui/components/IosIcons.kt
// Purpose: iOS-style icon presentation + runtime fallback logic.

package com.miui.dynamicisland.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val DefaultIosIconBg = Color(0xFF2C2C2E)

/** Subtle iOS-style inner highlight/shadow + thin border (drawn behind content). */
private fun Modifier.iosIconChrome(cornerRadius: Dp): Modifier = this.drawBehind {
    val r = cornerRadius.toPx().coerceAtLeast(0f)
    val cr = CornerRadius(r, r)

    // thin border
    val stroke = 1.dp.toPx().coerceAtLeast(1f)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        cornerRadius = cr,
        style = Stroke(width = stroke)
    )

    // top highlight
    val highlight = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.Transparent
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, size.height * 0.65f)
    )
    drawRoundRect(brush = highlight, cornerRadius = cr)

    // bottom shadow
    val shadow = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.22f)
        ),
        start = Offset(0f, size.height * 0.45f),
        end = Offset(0f, size.height)
    )
    drawRoundRect(brush = shadow, cornerRadius = cr)
}

@SuppressLint("DiscouragedApi")
fun findDrawableResId(context: Context, drawableName: String): Int {
    return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
}

fun findFirstDrawableResId(context: Context, candidates: List<String>): Int {
    for (name in candidates) {
        val id = findDrawableResId(context, name)
        if (id != 0) return id
    }
    return 0
}

/**
 * Tries to find an iOS-style replacement icon in `res/drawable`.
 */
@SuppressLint("DiscouragedApi")
fun findIosIconResId(context: Context, packageName: String): Int {
    val safePkg = packageName
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    val last = packageName.substringAfterLast('.').lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    val candidates = listOf(
        "ios_$safePkg",
        "ic_ios_$safePkg",
        "ios_$last",
        "ic_ios_$last",
    )

    for (name in candidates) {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id != 0) return id
    }
    return 0
}

private fun loadAppIconFromPackageManager(context: Context, packageName: String): Drawable? {
    return runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
}

/**
 * iOS-style app icon:
 *  - If an iOS replacement drawable exists -> uses painterResource.
 *  - Else falls back to the runtime app icon (using AsyncImage/Coil for robustness).
 *  - Else shows the app initial.
 */
@Composable
fun IosAppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    fallbackDrawable: Drawable? = null,
    backgroundColor: Color = DefaultIosIconBg,
    contentPadding: Dp = 2.dp,
) {
    val context = LocalContext.current
    val resId = remember(packageName) { findIosIconResId(context, packageName) }

    val iconData = remember(packageName, fallbackDrawable) {
        fallbackDrawable ?: loadAppIconFromPackageManager(context, packageName)
    }

    val radius = size * 0.28f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .iosIconChrome(radius),
        contentAlignment = Alignment.Center
    ) {
        when {
            resId != 0 -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resId)
                        .crossfade(true)
                        .build(),
                    contentDescription = appName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .clip(RoundedCornerShape(radius - 1.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            iconData != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(iconData)
                        .crossfade(true)
                        .build(),
                    contentDescription = appName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .clip(RoundedCornerShape(radius - 1.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                Text(
                    text = appName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** iOS-style glyph icon (Material ImageVector) inside a rounded premium container. */
@Composable
fun IosGlyphIcon(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerSize: Dp = 32.dp,
    iconSize: Dp = 20.dp,
    backgroundColor: Color = DefaultIosIconBg,
    tint: Color = Color.White,
) {
    val radius = containerSize * 0.28f
    Box(
        modifier = modifier
            .size(containerSize)
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .iosIconChrome(radius),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** iOS-style icon from drawable (prefer iOS pack assets); falls back to provided ImageVector. */
@Composable
fun IosDrawableOrGlyphIcon(
    drawableNameCandidates: List<String>,
    fallbackIcon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerSize: Dp = 32.dp,
    iconSize: Dp = 20.dp,
    backgroundColor: Color = DefaultIosIconBg,
    tint: Color = Color.White,
) {
    val context = LocalContext.current
    val resId = remember(drawableNameCandidates) { findFirstDrawableResId(context, drawableNameCandidates) }
    val radius = containerSize * 0.28f

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .iosIconChrome(radius),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(resId)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint)
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

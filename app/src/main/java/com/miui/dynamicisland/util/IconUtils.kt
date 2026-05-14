// File: app/src/main/java/com/miui/dynamicisland/util/IconUtils.kt

package com.miui.dynamicisland.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object IconUtils {

    /**
     * Converts an Android Drawable (including AdaptiveIcons) to a high-quality Bitmap.
     * Handles layers correctly to prevent distortion or silhouette issues.
     */
    fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
            }

            val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            
            // Scaled version for consistent size in the Island
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Helper for Compose to convert Drawable directly to ImageBitmap.
     */
    fun drawableToImageBitmap(drawable: Drawable?, sizePx: Int): ImageBitmap? {
        val d = drawable ?: return null
        return drawableToBitmap(d, sizePx, sizePx)?.asImageBitmap()
    }
}

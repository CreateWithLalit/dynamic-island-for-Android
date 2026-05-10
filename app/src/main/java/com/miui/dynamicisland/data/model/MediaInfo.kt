// File: app/src/main/java/com/miui/dynamicisland/data/model/MediaInfo.kt
// Purpose: Currently playing media information (title, artist, position, etc.)

package com.miui.dynamicisland.data.model

import android.graphics.Bitmap
import android.media.session.PlaybackState
import androidx.annotation.Keep
import java.util.Locale
import kotlin.math.roundToInt

@Keep
data class MediaInfo(
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArt: Bitmap? = null,
    val albumArtUri: String? = null,
    val duration: Long = 0L,
    val position: Long = 0L,
    val isPlaying: Boolean = false,
    val packageName: String = "",
    val playbackSpeed: Float = 1.0f,
    val shuffleMode: Int = 0,
    val repeatMode: Int = 0,
    val sessionId: Int = 0,
    val queueTitle: String? = null,
    val isActive: Boolean = true,
    val trackId: Long = 0L,
    val genre: String? = null,
    val year: Int = 0
) {

    val progress: Float
        get() = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedPosition: String get() = formatTime(position)
    val formattedDuration: String get() = formatTime(duration)

    val remainingTimeMs: Long
        get() = if (isPlaying && duration > 0L) (duration - position).coerceAtLeast(0L) else 0L

    val formattedRemainingTime: String
        get() = if (isPlaying && duration > 0L) "-${formatTime(remainingTimeMs)}" else formatTime(duration)

    // Naya position set karta hai (useful for real-time updates)
    fun withUpdatedPosition(newPosition: Long): MediaInfo {
        val safePosition = if (duration > 0L) newPosition.coerceIn(0L, duration) else newPosition.coerceAtLeast(0L)
        return copy(position = safePosition)
    }

    fun isSameTrack(other: MediaInfo): Boolean =
        title == other.title && artist == other.artist && album == other.album

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSeconds = (ms / 1000.0).roundToInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    companion object {
        val EMPTY = MediaInfo(title = "", artist = "", isActive = false)

        fun fromPlaybackState(
            title: String,
            artist: String,
            state: Int,
            position: Long,
            playbackSpeed: Float = 1.0f
        ): MediaInfo {
            return MediaInfo(
                title = title,
                artist = artist,
                position = position,
                isPlaying = state == PlaybackState.STATE_PLAYING,
                playbackSpeed = playbackSpeed
            )
        }
    }
}
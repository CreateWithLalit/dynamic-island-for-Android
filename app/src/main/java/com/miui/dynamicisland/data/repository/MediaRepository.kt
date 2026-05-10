// File: app/src/main/java/com/miui/dynamicisland/data/repository/MediaRepository.kt
// Purpose: Holds real-time media info and exposes media control stubs.
// Hinglish: Yahan MediaSession se media info aata hai aur controls handle hote hain.
//
// FIX: MediaInfo ka duplicate class hata diya – ab sirf data.model.MediaInfo use hogi.
//      play/pause/next/previous/seekTo stubs add kiye gaye (MediaController inject karke replace karo).

package com.miui.dynamicisland.data.repository

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.miui.dynamicisland.data.model.MediaInfo
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaRepository(private val context: Context) {

    companion object {
        private const val TAG = "MediaRepository"
    }

    // Real-time media state – updated by IslandNotificationListener or MediaSession callback
    private val _realTimeMediaInfo = MutableStateFlow<MediaInfo?>(null)
    val realTimeMediaInfo: StateFlow<MediaInfo?> = _realTimeMediaInfo.asStateFlow()

    // Active MediaController – set by IslandNotificationListener when a session becomes active
    @Volatile
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            IslandLogger.d(TAG, "Metadata changed", null)
            updateFromController(activeController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            IslandLogger.d(TAG, "Playback state changed: ${state?.state}", null)
            updateFromController(activeController)
        }

        override fun onSessionDestroyed() {
            IslandLogger.d(TAG, "Session destroyed", null)
            updateFromController(null)
        }
    }

    // ── Called by IslandNotificationListener when sessions change ──────────────
    fun updateFromController(controller: MediaController?) {
        if (activeController != controller) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = controller
            activeController?.registerCallback(controllerCallback)
        }

        if (controller == null) {
            _realTimeMediaInfo.value = null
            return
        }

        val meta = controller.metadata
        val pb   = controller.playbackState

        if (meta == null) {
            _realTimeMediaInfo.value = null
            return
        }

        val title    = meta.getString(MediaMetadata.METADATA_KEY_TITLE)     ?: ""
        val artist   = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)    ?: ""
        val album    = meta.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val artUri   = meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val position = pb?.position ?: 0L
        val isPlaying = pb?.state == PlaybackState.STATE_PLAYING

        val artBitmap = meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        _realTimeMediaInfo.value = MediaInfo(
            title        = title,
            artist       = artist,
            album        = album,
            albumArt     = artBitmap,
            albumArtUri  = artUri,
            duration     = duration,
            position     = position,
            isPlaying    = isPlaying,
            packageName  = controller.packageName,
            isActive     = true
        )
    }

    // ── Direct update (e.g. from polling / WorkManager) ───────────────────────
    fun updateMediaInfo(info: MediaInfo?) {
        _realTimeMediaInfo.value = info
    }

    // ── Playback controls ──────────────────────────────────────────────────────

    fun play() {
        val ctrl = activeController
        if (ctrl != null) {
            ctrl.transportControls.play()
            IslandLogger.d(TAG, "Play sent to ${ctrl.packageName}", null)
        } else {
            IslandLogger.w(TAG, "play() called but no active MediaController", null)
        }
    }

    fun pause() {
        val ctrl = activeController
        if (ctrl != null) {
            ctrl.transportControls.pause()
            IslandLogger.d(TAG, "Pause sent to ${ctrl.packageName}", null)
        } else {
            IslandLogger.w(TAG, "pause() called but no active MediaController", null)
        }
    }

    fun next() {
        val ctrl = activeController
        if (ctrl != null) {
            ctrl.transportControls.skipToNext()
            IslandLogger.d(TAG, "Next sent to ${ctrl.packageName}", null)
        } else {
            IslandLogger.w(TAG, "next() called but no active MediaController", null)
        }
    }

    fun previous() {
        val ctrl = activeController
        if (ctrl != null) {
            ctrl.transportControls.skipToPrevious()
            IslandLogger.d(TAG, "Previous sent to ${ctrl.packageName}", null)
        } else {
            IslandLogger.w(TAG, "previous() called but no active MediaController", null)
        }
    }

    fun seekTo(positionMs: Long) {
        val ctrl = activeController
        if (ctrl != null) {
            ctrl.transportControls.seekTo(positionMs)
            IslandLogger.d(TAG, "Seek to $positionMs ms in ${ctrl.packageName}", null)
        } else {
            IslandLogger.w(TAG, "seekTo() called but no active MediaController", null)
        }
    }
}
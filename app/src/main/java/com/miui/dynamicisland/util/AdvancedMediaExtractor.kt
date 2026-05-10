package com.miui.dynamicisland.util

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.StatusBarNotification
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.service.IslandNotificationListener
import com.miui.dynamicisland.ui.states.IslandState

class AdvancedMediaExtractor(
    context: Context,
    private val stateManager: IslandStateManager
) {
    companion object {
        private const val TAG = "AdvancedMediaExtractor"
    }

    private val appContext = context.applicationContext
    private var activeMediaController: MediaController? = null

    private val mediaSessionManager: MediaSessionManager by lazy {
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaState()
        }
    }

    fun extractMediaFromNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable("android.mediaSession", MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable("android.mediaSession")
        }

        if (token != null) {
            connectToMediaSession(token)
        } else {
            extractBasicMediaInfo(extras)
        }
    }

    private fun connectToMediaSession(token: MediaSession.Token) {
        try {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = MediaController(appContext, token).apply {
                registerCallback(mediaCallback)
            }
            updateMediaState()
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Failed to connect media session: ${e.message}", e)
            activeMediaController = null
        }
    }

    private fun updateMediaState() {
        val controller = activeMediaController ?: findActiveMediaController() ?: return
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().ifBlank { "Playing..." }
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"

        val albumArtUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val position = playbackState?.position ?: 0L
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        stateManager.pushState(
            IslandState.Media(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                packageName = controller.packageName,
                albumArtUri = albumArtUri,
                duration = duration,
                position = position
            )
        )
    }

    private fun findActiveMediaController(): MediaController? {
        return try {
            val component = ComponentName(appContext, IslandNotificationListener::class.java)
            mediaSessionManager.getActiveSessions(component)
                .firstOrNull()
                ?.also { controller ->
                    activeMediaController = controller
                    controller.registerCallback(mediaCallback)
                }
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Failed to find active media controller: ${e.message}", e)
            null
        }
    }

    private fun extractBasicMediaInfo(extras: android.os.Bundle) {
        val title = extras.getCharSequence("android.title")?.toString().orEmpty().ifBlank { "Media" }
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        stateManager.pushState(
            IslandState.Media(
                title = title,
                artist = text.ifBlank { "Unknown Artist" },
                isPlaying = true,
                packageName = "",
                albumArtUri = null,
                duration = 0L,
                position = 0L
            )
        )
    }

    fun cleanup() {
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = null
    }
}
package com.miui.dynamicisland.data.repository

import android.media.session.MediaController
import com.miui.dynamicisland.util.IslandLogger
import java.lang.ref.WeakReference

object MediaRepositoryBridge {

    private const val TAG = "MediaRepositoryBridge"
    private var mediaRepositoryRef: WeakReference<MediaRepository>? = null

    fun register(repository: MediaRepository) {
        mediaRepositoryRef = WeakReference(repository)
        IslandLogger.d(TAG, "MediaRepository registered", null)
    }

    fun unregister() {
        mediaRepositoryRef?.clear()
        mediaRepositoryRef = null
        IslandLogger.d(TAG, "MediaRepository unregistered", null)
    }

    fun updateFromController(controller: MediaController?) {
        repository()?.updateFromController(controller)
            ?: IslandLogger.w(TAG, "No MediaRepository registered — skipping update", null)
    }

    fun togglePlayPause() {
        val repo = repository()
        if (repo == null) {
            IslandLogger.w(TAG, "togglePlayPause() called but no MediaRepository registered", null)
            return
        }
        val current = repo.realTimeMediaInfo.value
        if (current?.isPlaying == true) repo.pause() else repo.play()
    }

    fun next() {
        repository()?.next()
            ?: IslandLogger.w(TAG, "next() called but no MediaRepository registered", null)
    }

    fun previous() {
        repository()?.previous()
            ?: IslandLogger.w(TAG, "previous() called but no MediaRepository registered", null)
    }

    fun seekTo(positionFraction: Float) {
        val repo = repository()
        if (repo == null) {
            IslandLogger.w(TAG, "seekTo() called but no MediaRepository registered", null)
            return
        }
        val current = repo.realTimeMediaInfo.value ?: return
        if (current.duration > 0L) {
            repo.seekTo((positionFraction * current.duration).toLong())
        }
    }

    private fun repository(): MediaRepository? = mediaRepositoryRef?.get()
}
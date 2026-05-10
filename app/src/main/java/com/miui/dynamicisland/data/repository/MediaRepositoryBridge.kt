package com.miui.dynamicisland.data.repository

import android.media.session.MediaController
import com.miui.dynamicisland.util.IslandLogger

object MediaRepositoryBridge {

    private const val TAG = "MediaRepositoryBridge"
    private var mediaRepository: MediaRepository? = null

    fun register(repository: MediaRepository) {
        mediaRepository = repository
        IslandLogger.d(TAG, "MediaRepository registered", null)
    }

    fun unregister() {
        mediaRepository = null
        IslandLogger.d(TAG, "MediaRepository unregistered", null)
    }

    fun updateFromController(controller: MediaController?) {
        mediaRepository?.updateFromController(controller)
            ?: IslandLogger.w(TAG, "No MediaRepository registered — skipping update", null)
    }
}
// File: app/src/main/java/com/miui/dynamicisland/util/IslandLogger.kt

package com.miui.dynamicisland.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object IslandLogger {

    private const val TAG = "DynamicIsland"
    private const val MAX_LOG_FILE_SIZE_BYTES = 5 * 1024 * 1024L
    private const val MAX_LOG_FILES = 3
    private const val LOG_FILE_PREFIX = "island_log"
    private const val LOG_FILE_EXTENSION = ".txt"

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeInProgress = AtomicBoolean(false)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var logDirectory: File? = null
    private var fileLoggingEnabled = false
    private var isEnabled = true

    enum class Level(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }

    fun initialize(context: Context, enableFileLogging: Boolean = true) {
        isEnabled = true
        fileLoggingEnabled = enableFileLogging
        if (enableFileLogging) {
            logDirectory = File(context.applicationContext.filesDir, "logs").apply { mkdirs() }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    // MANDATORY 3-PARAM SIGNATURE per Section 5 instructions
    fun d(tag: String, message: String, throwable: Throwable?) = log(Level.DEBUG, tag, throwable, message)
    fun i(tag: String, message: String, throwable: Throwable?) = log(Level.INFO, tag, throwable, message)
    fun w(tag: String, message: String, throwable: Throwable?) = log(Level.WARN, tag, throwable, message)
    fun v(tag: String, message: String, throwable: Throwable?) = log(Level.VERBOSE, tag, throwable, message)
    fun e(tag: String, message: String, throwable: Throwable?) = log(Level.ERROR, tag, throwable, message)

    // Alias for .log() as per Section 4/5 pseudocode
    fun log(tag: String, message: String, throwable: Throwable?) = log(Level.DEBUG, tag, throwable, message)

    private fun log(level: Level, tag: String, throwable: Throwable?, message: String) {
        if (!isEnabled) return

        val finalTag = "$TAG:$tag"

        when (level) {
            Level.VERBOSE -> Log.v(finalTag, message, throwable)
            Level.DEBUG -> Log.d(finalTag, message, throwable)
            Level.INFO -> Log.i(finalTag, message, throwable)
            Level.WARN -> Log.w(finalTag, message, throwable)
            Level.ERROR -> Log.e(finalTag, message, throwable)
        }

        val fileMessage = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
        writeToFile(level, "[$tag] $fileMessage")
    }

    private fun writeToFile(level: Level, message: String) {
        if (!fileLoggingEnabled || !isEnabled) return
        ioScope.launch {
            if (!writeInProgress.compareAndSet(false, true)) return@launch
            try {
                val directory = logDirectory ?: return@launch
                val logFile = File(directory, "$LOG_FILE_PREFIX$LOG_FILE_EXTENSION")
                val timestamp = dateFormat.format(Date())
                val logLine = "$timestamp [${level.name}] $message\n"

                if (logFile.exists() && logFile.length() + logLine.toByteArray().size > MAX_LOG_FILE_SIZE_BYTES) {
                    rotateLogFiles(directory)
                }
                FileWriter(logFile, true).use { writer -> writer.write(logLine) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log file", e)
            } finally {
                writeInProgress.set(false)
            }
        }
    }

    private fun rotateLogFiles(directory: File) {
        val oldestFile = File(directory, "$LOG_FILE_PREFIX.$MAX_LOG_FILES$LOG_FILE_EXTENSION")
        if (oldestFile.exists()) oldestFile.delete()
        for (index in MAX_LOG_FILES - 1 downTo 1) {
            val oldFile = File(directory, "$LOG_FILE_PREFIX.$index$LOG_FILE_EXTENSION")
            val newFile = File(directory, "$LOG_FILE_PREFIX.${index + 1}$LOG_FILE_EXTENSION")
            if (oldFile.exists()) oldFile.renameTo(newFile)
        }
        val currentFile = File(directory, "$LOG_FILE_PREFIX$LOG_FILE_EXTENSION")
        if (currentFile.exists()) currentFile.renameTo(File(directory, "$LOG_FILE_PREFIX.1$LOG_FILE_EXTENSION"))
    }

    fun getLogFiles(): List<File> = logDirectory?.listFiles()
        ?.filter { it.name.startsWith(LOG_FILE_PREFIX) && it.name.endsWith(LOG_FILE_EXTENSION) }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()

    suspend fun clearLogs() = withContext(Dispatchers.IO) { getLogFiles().forEach { it.delete() } }
}

package com.port2pullman.app.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global singleton that collects debug/error messages at runtime.
 * Any class can call [DebugLog.d] or [DebugLog.e] to append entries.
 * The UI can observe [entries] to display them in the debug console.
 */
object DebugLog {

    data class Entry(
        val timestamp: String,
        val tag: String,
        val level: Level,
        val message: String,
    )

    enum class Level { DEBUG, INFO, WARN, ERROR }

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    /** Log a debug message. */
    fun d(tag: String, message: String) = log(tag, Level.DEBUG, message)

    /** Log an info message. */
    fun i(tag: String, message: String) = log(tag, Level.INFO, message)

    /** Log a warning. */
    fun w(tag: String, message: String) = log(tag, Level.WARN, message)

    /** Log an error. */
    fun e(tag: String, message: String) = log(tag, Level.ERROR, message)

    /** Log an error with throwable. */
    fun e(tag: String, message: String, throwable: Throwable) =
        log(tag, Level.ERROR, "$message\n  → ${throwable::class.simpleName}: ${throwable.message}")

    private fun log(tag: String, level: Level, message: String) {
        val entry = Entry(
            timestamp = fmt.format(Date()),
            tag = tag,
            level = level,
            message = message,
        )
        _entries.value = _entries.value + entry
        // Also print to logcat
        android.util.Log.println(
            when (level) {
                Level.DEBUG -> android.util.Log.DEBUG
                Level.INFO -> android.util.Log.INFO
                Level.WARN -> android.util.Log.WARN
                Level.ERROR -> android.util.Log.ERROR
            },
            "P2P/$tag",
            message
        )
    }

    /** Clear all entries. */
    fun clear() {
        _entries.value = emptyList()
    }
}

package com.port2pullman.app.debug

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global uncaught-exception handler that persists crash details to
 * `files/crash_log.txt`.  On next launch call [loadPreviousCrash]
 * to replay the crash into [DebugLog] so the user can see it in
 * the debug console.
 */
object CrashHandler {

    private const val FILE_NAME = "crash_log.txt"
    private const val TAG = "CRASH"
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Install as the default uncaught-exception handler.
     * Must be called from [android.app.Application.onCreate].
     */
    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildReport(thread, throwable)
                // Write to internal storage (survives process death)
                File(context.filesDir, FILE_NAME).writeText(report)
                // Also push to DebugLog in case the process survives long
                // enough for in-memory state to be useful
                DebugLog.e(TAG, report)
            } catch (_: Exception) {
                // Best effort — don't let logging crash the crash handler
            }
            // Delegate to the system default (shows "app has stopped" dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * If a crash log exists from a previous session, replay it into
     * [DebugLog] so it appears at the top of the debug console,
     * then delete the file.
     */
    fun loadPreviousCrash(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return

        val report = file.readText()
        if (report.isNotBlank()) {
            DebugLog.e(TAG, "═══ PREVIOUS CRASH ═══")
            // Split into lines and log each so long traces are readable
            for (line in report.lines()) {
                DebugLog.e(TAG, line)
            }
            DebugLog.e(TAG, "═══ END CRASH LOG ═══")
        }
        file.delete()
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        sw.appendLine("Crash at ${fmt.format(Date())}")
        sw.appendLine("Thread: ${thread.name} (id=${thread.id})")
        sw.appendLine()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}

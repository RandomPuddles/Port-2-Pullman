package com.port2pullman.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.media.Ringtone
import com.port2pullman.app.debug.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the "dismiss" action from a ringing alarm notification.
 * Stops the looping ringtone, cancels the notification, and plays
 * any pending TTS readout after the ringtone has stopped.
 */
class AlarmDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.port2pullman.app.ACTION_DISMISS_ALARM"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "AlarmDismiss"

        /** Active ringtones keyed by notification ID so we can stop them. */
        private val activeRingtones = mutableMapOf<Int, Ringtone>()

        /** Pending TTS text keyed by notification ID — played after dismiss. */
        private val pendingTts = mutableMapOf<Int, String>()

        /** TTSClient instance registered by AlarmEvaluatorService. */
        @Volatile
        var ttsClient: TTSClient? = null

        fun registerRingtone(notificationId: Int, ringtone: Ringtone) {
            activeRingtones[notificationId] = ringtone
        }

        fun stopRingtone(notificationId: Int) {
            activeRingtones.remove(notificationId)?.let {
                if (it.isPlaying) it.stop()
                DebugLog.d(TAG, "Stopped ringtone for notification $notificationId")
            }
        }

        /**
         * Register TTS text to be spoken after the ringing alarm is dismissed.
         */
        fun registerPendingTts(notificationId: Int, text: String) {
            pendingTts[notificationId] = text
            DebugLog.d(TAG, "Registered pending TTS for notification $notificationId")
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        DebugLog.i(TAG, "Dismiss received for notification $notificationId")

        // Stop the ringtone
        stopRingtone(notificationId)

        // Cancel the notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)

        // Play any pending TTS readout now that the ringtone is silent
        val ttsText = pendingTts.remove(notificationId)
        if (ttsText != null && ttsClient != null) {
            DebugLog.i(TAG, "Playing deferred TTS readout for notification $notificationId")
            scope.launch {
                try {
                    ttsClient?.speak(ttsText)
                } catch (e: Exception) {
                    DebugLog.e(TAG, "Deferred TTS failed: ${e.message}")
                }
            }
        }
    }
}

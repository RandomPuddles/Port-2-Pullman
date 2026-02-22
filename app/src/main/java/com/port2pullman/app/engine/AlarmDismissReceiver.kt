package com.port2pullman.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.media.Ringtone
import android.media.RingtoneManager
import com.port2pullman.app.debug.DebugLog

/**
 * Receives the "dismiss" action from a ringing alarm notification.
 * Stops the looping ringtone and cancels the notification.
 */
class AlarmDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.port2pullman.app.ACTION_DISMISS_ALARM"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "AlarmDismiss"

        /** Active ringtones keyed by notification ID so we can stop them. */
        private val activeRingtones = mutableMapOf<Int, Ringtone>()

        fun registerRingtone(notificationId: Int, ringtone: Ringtone) {
            activeRingtones[notificationId] = ringtone
        }

        fun stopRingtone(notificationId: Int) {
            activeRingtones.remove(notificationId)?.let {
                if (it.isPlaying) it.stop()
                DebugLog.d(TAG, "Stopped ringtone for notification $notificationId")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        DebugLog.i(TAG, "Dismiss received for notification $notificationId")

        // Stop the ringtone
        stopRingtone(notificationId)

        // Cancel the notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
    }
}

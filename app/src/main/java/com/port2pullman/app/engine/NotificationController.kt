package com.port2pullman.app.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.port2pullman.app.MainActivity
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.model.Alarm

/**
 * Manages notification display for triggered alarms.
 */
class NotificationController(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "alarm_triggers"
        const val CHANNEL_NAME = "Alarm Triggers"
        const val RING_CHANNEL_ID = "alarm_ring"
        const val RING_CHANNEL_NAME = "Ringing Alarms"
        private const val TAG = "NotifCtrl"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Regular alarm triggers
        val standard = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for triggered conditional alarms"
            enableVibration(true)
        }
        nm.createNotificationChannel(standard)

        // Ringing alarms — max importance, alarm sound, no timeout
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val ring = NotificationChannel(
            RING_CHANNEL_ID,
            RING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Continuous alarm until dismissed"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setSound(alarmUri, audioAttrs)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ring)
    }

    /**
     * Show a notification for a triggered alarm.
     *
     * When [Alarm.ring] is `true`, the notification plays the device
     * alarm ringtone in a loop and shows a "Dismiss" action. The
     * ringtone continues until the user taps Dismiss.
     */
    fun showTriggered(alarm: Alarm) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = alarm.id.toInt()

        if (alarm.ring) {
            showRingingAlarm(alarm, nm, notificationId)
        } else {
            showStandardNotification(alarm, nm, notificationId)
        }
    }

    // ─── Standard notification ──────────────────────────────────────

    private fun showStandardNotification(alarm: Alarm, nm: NotificationManager, notificationId: Int) {
        val tapIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val icon = if (alarm.readout) android.R.drawable.ic_btn_speak_now
                   else android.R.drawable.ic_popup_reminder

        val subtitle = if (alarm.readout) "Reading title aloud" else alarm.title

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(alarm.title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(notificationId, notification)
    }

    // ─── Ringing alarm ──────────────────────────────────────────────

    private fun showRingingAlarm(alarm: Alarm, nm: NotificationManager, notificationId: Int) {
        DebugLog.i(TAG, "Starting ringing alarm for '${alarm.title}' (id=$notificationId)")

        // Full-screen intent to bring the app to the foreground
        val fullScreenIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Dismiss action → stops ringtone & cancels notification
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            action = AlarmDismissReceiver.ACTION_DISMISS
            putExtra(AlarmDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, notificationId,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val subtitle = if (alarm.readout)
            "Alarm ringing — readout will follow"
        else
            "Alarm ringing — tap Dismiss to stop"

        val notification = NotificationCompat.Builder(context, RING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ ${alarm.title}")
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)                       // Can't be swiped away
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPending
            )
            // Delete intent as fallback (if notification is somehow cleared)
            .setDeleteIntent(dismissPending)
            .build()

        nm.notify(notificationId, notification)

        // Start looping the alarm ringtone
        startRingtone(notificationId)
    }

    private fun startRingtone(notificationId: Int) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return

            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.isLooping = true
                ringtone.play()
                AlarmDismissReceiver.registerRingtone(notificationId, ringtone)
                DebugLog.d(TAG, "Ringtone started (looping) for notification $notificationId")
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "Failed to start ringtone", e)
        }
    }
}

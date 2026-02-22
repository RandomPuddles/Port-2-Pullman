package com.port2pullman.app.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.port2pullman.app.R
import com.port2pullman.app.model.Alarm

/**
 * Manages notification display for triggered alarms.
 */
class NotificationController(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "alarm_triggers"
        const val CHANNEL_NAME = "Alarm Triggers"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for triggered conditional alarms"
            enableVibration(true)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /**
     * Show a notification for a triggered alarm.
     */
    fun showTriggered(alarm: Alarm) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val icon = when {
            alarm.ring -> android.R.drawable.ic_popup_reminder
            alarm.readout -> android.R.drawable.ic_btn_speak_now
            else -> android.R.drawable.ic_popup_reminder
        }

        val subtitle = when {
            alarm.ring && alarm.readout -> "Ringing… (readout will play after dismiss)"
            alarm.ring -> "Alarm ringing until dismissed"
            alarm.readout -> "Reading title aloud"
            else -> alarm.title
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(alarm.title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(alarm.id.toInt(), notification)
    }
}

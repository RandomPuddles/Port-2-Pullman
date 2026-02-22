package com.port2pullman.app.engine

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.port2pullman.app.App
import com.port2pullman.app.MainActivity
import com.port2pullman.app.data.AlarmRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground service that periodically evaluates all enabled alarms.
 * When a condition tree evaluates to true the alarm is "triggered"
 * via [NotificationController].
 */
class AlarmEvaluatorService : Service() {

    companion object {
        const val CHANNEL_ID = "alarm_service"
        const val CHANNEL_NAME = "Alarm Monitor"
        const val NOTIFICATION_ID = 1001
        private const val EVAL_INTERVAL_MS = 60_000L // 1 minute
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationController: NotificationController
    private lateinit var alarmRepo: AlarmRepositoryImpl
    private val evaluator = ConditionTreeEvaluator()

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as App
        alarmRepo = app.alarmRepository as AlarmRepositoryImpl
        notificationController = NotificationController(this)
        createChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startEvaluationLoop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the alarm evaluation engine running"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Conditional Alarms")
            .setContentText("Monitoring alarm conditions…")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startEvaluationLoop() {
        scope.launch {
            while (isActive) {
                try {
                    evaluateAll()
                } catch (_: Exception) { /* swallow */ }
                delay(EVAL_INTERVAL_MS)
            }
        }
    }

    private suspend fun evaluateAll() {
        val alarms = alarmRepo.getAll().first()
        for (alarm in alarms) {
            if (!alarm.enabled) continue
            val triggered = evaluator.evaluate(alarm.rootCondition)
            if (triggered) {
                notificationController.showTriggered(alarm)
                if (alarm.triggerOnce) {
                    alarmRepo.setEnabled(listOf(alarm.id), false)
                }
            }
        }
    }
}

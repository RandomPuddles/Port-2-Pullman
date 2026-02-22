package com.port2pullman.app.engine

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.port2pullman.app.App
import com.port2pullman.app.MainActivity
import com.port2pullman.app.data.AlarmRepositoryImpl
import com.port2pullman.app.data.TriggerHistoryDao
import com.port2pullman.app.data.TriggerHistoryEntity
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.debug.DebugSettings
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
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationController: NotificationController
    private lateinit var alarmRepo: AlarmRepositoryImpl
    private lateinit var evaluator: ConditionTreeEvaluator
    private lateinit var triggerHistoryDao: TriggerHistoryDao

    override fun onCreate() {
        super.onCreate()
        DebugLog.i("EvalService", "Service onCreate")
        val app = applicationContext as App
        alarmRepo = app.alarmRepository as AlarmRepositoryImpl
        triggerHistoryDao = app.triggerHistoryDao
        notificationController = NotificationController(this)
        evaluator = ConditionTreeEvaluator(this, triggerHistoryDao)
        LocationProvider.start(this)
        createChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startEvaluationLoop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        DebugLog.i("EvalService", "Service onDestroy")
        LocationProvider.stop(this)
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
            DebugLog.i("EvalService", "Evaluation loop started (interval=${DebugSettings.evalIntervalMs}ms)")
            while (isActive) {
                try {
                    evaluateAll()
                } catch (e: Exception) {
                    DebugLog.e("EvalService", "Error during evaluation", e)
                }
                delay(DebugSettings.evalIntervalMs)
            }
        }
    }

    private suspend fun evaluateAll() {
        // Pre-fetch weather so data is current before any condition checks.
        // Without this, ensureFresh() fires-and-forgets and evaluations
        // read stale cached values (especially problematic in background).
        try {
            WeatherProvider.fetchNow(this@AlarmEvaluatorService, forceRefresh = false)
        } catch (e: Exception) {
            DebugLog.w("EvalService", "Weather pre-fetch failed: ${e.message}")
        }

        val alarms = alarmRepo.getAll().first()
        val enabled = alarms.filter { it.enabled }
        DebugLog.d("EvalService", "evaluateAll: ${enabled.size}/${alarms.size} alarms enabled")

        for (alarm in enabled) {
            val triggered = evaluator.evaluate(alarm.rootCondition, alarm.lastStartedAt, alarm.id)
            DebugLog.d("EvalService", "Alarm '${alarm.title}' (id=${alarm.id}): triggered=$triggered")
            if (triggered) {
                DebugLog.i("EvalService", "TRIGGERED: '${alarm.title}'")
                notificationController.showTriggered(alarm)
                // Record trigger in history for limit conditions
                triggerHistoryDao.insert(TriggerHistoryEntity(alarmId = alarm.id))
                DebugLog.d("EvalService", "Recorded trigger history for alarm ${alarm.id}")
                // Reset the reference time so time-based conditions
                // don't keep triggering on every evaluation cycle
                alarmRepo.resetLastStartedAt(alarm.id)
                DebugLog.d("EvalService", "Reset lastStartedAt for alarm ${alarm.id}")
                if (alarm.triggerOnce) {
                    DebugLog.d("EvalService", "triggerOnce - disabling alarm ${alarm.id}")
                    alarmRepo.setEnabled(listOf(alarm.id), false)
                }
            }
        }
    }
}

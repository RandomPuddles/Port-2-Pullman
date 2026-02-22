package com.example.eventtriggeralarm.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.eventtriggeralarm.MainActivity
import com.example.eventtriggeralarm.R
import com.example.eventtriggeralarm.data.Alarm
import com.example.eventtriggeralarm.data.AlarmRepository
import com.example.eventtriggeralarm.domain.condition.TreeBuilder
import com.example.eventtriggeralarm.evaluator.StubGeminiEvaluator

class AlarmEvaluationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = AlarmRepository(context.applicationContext)
    private val geminiEvaluator = StubGeminiEvaluator()

    override suspend fun doWork(): Result {
        val alarms = repository.getAlarms()
        if (alarms.isEmpty()) return Result.success()

        val context = applicationContext
        var updated = false
        val updatedAlarms = alarms.toMutableList()

        for ((index, alarm) in alarms.withIndex()) {
            if (!alarm.enabled) continue
            if (alarm.conditions.isEmpty()) continue

            val satisfied = runCatching {
                val tree = TreeBuilder.buildTree(
                    items = alarm.conditions,
                    operators = alarm.operators,
                    context = context,
                    geminiEvaluator = geminiEvaluator
                )
                val hasCustom = alarm.conditions.any { it.custom }
                if (hasCustom) {
                    val phase1 = tree.getCondition(skipCustom = true)
                    if (!phase1) false else tree.getCondition(skipCustom = false)
                } else {
                    tree.getCondition(skipCustom = false)
                }
            }.getOrElse { false }

            if (satisfied) {
                showNotification(context, alarm)
                if (alarm.triggerOnce) {
                    updatedAlarms[index] = alarm.copy(enabled = false)
                    updated = true
                }
                break // One trigger per run
            }
        }

        if (updated) {
            repository.saveAlarms(updatedAlarms)
        }

        return Result.success()
    }

    private fun showNotification(context: Context, alarm: Alarm) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt() and 0xFFFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when {
            alarm.ring && alarm.readout -> "🔔 Ringing… (tap to open)"
            alarm.ring -> "🔔 Alarm ringing"
            alarm.readout -> "🗣️ Reading aloud"
            else -> "📌 ${alarm.title}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + (alarm.id.toInt() and 0xFFFF), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_alarms),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alarms_desc)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "event_trigger_alarm"
        private const val NOTIFICATION_ID_BASE = 1000
    }
}

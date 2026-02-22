package com.example.eventtriggeralarm

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.eventtriggeralarm.worker.AlarmEvaluationWorker
import java.util.concurrent.TimeUnit

class EventTriggerAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleAlarmEvaluation()
    }

    private fun scheduleAlarmEvaluation() {
        val request = PeriodicWorkRequestBuilder<AlarmEvaluationWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "alarm_evaluation",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

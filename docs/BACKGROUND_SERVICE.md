# Background Service

## Overview

`EvaluationWorker` is a `CoroutineWorker` managed by WorkManager. It runs on the schedule defined by each reminder, evaluates the condition via `GeminiEvaluator`, and fires a notification if the condition is met.

---

## EvaluationWorker

```kotlin
@HiltWorker
class EvaluationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ReminderRepository,
    private val evaluator: GeminiEvaluator,
    private val notificationService: NotificationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()

        val reminder = repository.getById(reminderId) ?: return Result.success()
        if (!reminder.isActive) return Result.success()

        val conditionMet = evaluator.evaluate(reminder.conditionPrompt)

        if (conditionMet) {
            notificationService.fire(reminder)
            if (reminder.triggerOnce) {
                repository.deactivate(reminderId)
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}
```

---

## SchedulerService

`SchedulerService` is a helper that wraps WorkManager scheduling logic. Call it whenever a reminder is created, updated, or deleted.

```kotlin
@Singleton
class SchedulerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(reminder: Reminder) {
        val inputData = workDataOf(EvaluationWorker.KEY_REMINDER_ID to reminder.id)

        when (val schedule = reminder.schedule) {

            is Schedule.Interval -> {
                val request = PeriodicWorkRequestBuilder<EvaluationWorker>(
                    schedule.intervalMinutes, TimeUnit.MINUTES
                )
                    .setInputData(inputData)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag(reminder.id)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    reminder.id,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }

            is Schedule.OneShot -> {
                val delayMs = schedule.triggerAtMs - System.currentTimeMillis()
                if (delayMs <= 0) return  // already past

                val request = OneTimeWorkRequestBuilder<EvaluationWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag(reminder.id)
                    .build()

                workManager.enqueueUniqueWork(
                    reminder.id,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }

    fun cancel(reminderId: String) {
        workManager.cancelUniqueWork(reminderId)
    }
}
```

---

## NotificationService

```kotlin
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null

    fun fire(reminder: Reminder) {
        if (reminder.notificationMethod.voiceOutput) speakAloud(reminder.title)
        if (reminder.notificationMethod.ringing) ring()
        showNotification(reminder)
    }

    private fun speakAloud(text: String) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun ring() {
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone?.play()
    }

    private fun showNotification(reminder: Reminder) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(reminder.title)
            .setContentText(reminder.conditionPrompt)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            reminder.id.hashCode(),
            notification
        )
    }

    companion object {
        const val CHANNEL_ID = "condition_alarm_channel"
    }
}
```

---

## Notification Channel Setup

Create the notification channel in `Application.onCreate()`:

```kotlin
@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationService.CHANNEL_ID,
                "Condition Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires when your alarm conditions are met"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
```

---

## AndroidManifest Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

---

## WorkManager Constraints

`NetworkType.CONNECTED` is set as a constraint on all workers. This means:
- Evaluations are skipped if there is no internet — the worker is rescheduled by WorkManager automatically
- Gemini calls are never attempted offline
- WorkManager retries when connectivity is restored

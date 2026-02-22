package com.port2pullman.app

import android.app.Application
import com.port2pullman.app.data.*

/**
 * Application class providing singleton access to database and repositories.
 */
class App : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    val alarmRepository: IAlarmRepository by lazy {
        AlarmRepositoryImpl(database.alarmDao())
    }

    val conditionRepository: IConditionRepository by lazy {
        ConditionRepositoryImpl(database.conditionDao())
    }

    val triggerHistoryDao by lazy { database.triggerHistoryDao() }

    override fun onCreate() {
        super.onCreate()
        // Parse the condition catalog from res/raw/conditions.json
        ConditionRegistry.init(this)
    }
}

package com.example.eventtriggeralarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alarms")

class AlarmRepository(private val context: Context) {

    private val gson = Gson()
    private val alarmsType = object : TypeToken<List<Alarm>>() {}.type

    val alarms: Flow<List<Alarm>> = context.dataStore.data.map { prefs ->
        val json = prefs[ALARMS_KEY] ?: "[]"
        runCatching {
            gson.fromJson<List<Alarm>>(json, alarmsType) ?: emptyList()
        }.getOrElse { emptyList() }
    }

    suspend fun saveAlarms(alarms: List<Alarm>) {
        context.dataStore.edit { prefs ->
            prefs[ALARMS_KEY] = gson.toJson(alarms)
        }
    }

    suspend fun getAlarms(): List<Alarm> = alarms.first()

    companion object {
        private val ALARMS_KEY = stringPreferencesKey("alarms")
    }
}

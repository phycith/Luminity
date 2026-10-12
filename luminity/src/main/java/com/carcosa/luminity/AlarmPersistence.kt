package com.carcosa.luminity

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmPersistence(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("luminity_alarms", Context.MODE_PRIVATE)
    private val gson = Gson()

    data class PersistableAlarm(
        val id: String,
        val hour: Int,
        val minute: Int,
        val isEnabled: Boolean,
        val selectedDays: Set<Int>,
        val everyXDays: Int?,
        val referenceDateMillis: Long,
        val snoozeLimit: Int,
        val selectedMissions: List<String>, // Store enum names
        val difficultyName: String,         // Store enum name
        val typingSentences: List<String>,
        val typingRandomRounds: Int,
        val typingUseSentences: Boolean,
        val typingUseRandom: Boolean,
        val isWakeUpCheckEnabled: Boolean,
        val qrCodeData: String?,
        val volume: Float,
        val isVibrationEnabled: Boolean
    )


    fun saveAlarms(alarms: List<Alarm>) {
        try {
            val persistable = alarms.map { alarm ->
                PersistableAlarm(
                    id = alarm.id,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    isEnabled = alarm.isEnabled,
                    selectedDays = alarm.selectedDays,
                    everyXDays = alarm.everyXDays,
                    referenceDateMillis = alarm.referenceDateMillis,
                    snoozeLimit = alarm.snoozeLimit,
                    selectedMissions = alarm.selectedMissions.map { it.name },
                    difficultyName = alarm.difficulty.name,
                    typingSentences = alarm.typingSentences,
                    typingRandomRounds = alarm.typingRandomRounds,
                    typingUseSentences = alarm.typingUseSentences,
                    typingUseRandom = alarm.typingUseRandom,
                    isWakeUpCheckEnabled = alarm.isWakeUpCheckEnabled,
                    qrCodeData = alarm.qrCodeData,
                    volume = alarm.volume,
                    isVibrationEnabled = alarm.isVibrationEnabled
                )
            }

            val json = gson.toJson(persistable)
            sharedPreferences.edit().putString("alarms_key", json).apply()
            Log.d("AlarmPersistence", "Saved ${alarms.size} alarms")
        } catch (e: Exception) {
            Log.e("AlarmPersistence", "Error saving alarms", e)
        }
    }

    fun loadAlarms(): List<Alarm> {
        val json = sharedPreferences.getString("alarms_key", null) ?: return emptyList()
        val type = object : TypeToken<List<PersistableAlarm>>() {}.type
        return try {
            val persistable: List<PersistableAlarm> = gson.fromJson(json, type)
            persistable.map { p ->
                Alarm(
                    id = p.id,
                    hour = p.hour,
                    minute = p.minute,
                    isEnabled = p.isEnabled,
                    selectedDays = p.selectedDays,
                    everyXDays = p.everyXDays,
                    referenceDateMillis = p.referenceDateMillis,
                    snoozeLimit = p.snoozeLimit,
                    selectedMissions = p.selectedMissions.mapNotNull { 
                        try { MissionType.valueOf(it) } catch (_: Exception) { null }
                    },
                    difficulty = try { Difficulty.valueOf(p.difficultyName) } catch (_: Exception) { Difficulty.NORMAL },
                    typingSentences = p.typingSentences,
                    typingRandomRounds = p.typingRandomRounds,
                    typingUseSentences = p.typingUseSentences,
                    typingUseRandom = p.typingUseRandom,
                    isWakeUpCheckEnabled = p.isWakeUpCheckEnabled,
                    qrCodeData = p.qrCodeData,
                    volume = p.volume,
                    isVibrationEnabled = p.isVibrationEnabled
                )
            }.also {

                Log.d("AlarmPersistence", "Loaded ${it.size} alarms")
            }
        } catch (e: Exception) {
            Log.e("AlarmPersistence", "Error loading alarms", e)
            emptyList()
        }
    }
}


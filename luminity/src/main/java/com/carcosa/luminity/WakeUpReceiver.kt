package com.carcosa.luminity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WakeUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("ALARM_ID") ?: return
        
        when (intent.action) {
            "START_WAKE_UP_CHECK" -> {
                Log.d("WakeUpReceiver", "Starting wake-up check for $alarmId")
                NotificationHelper.showWakeUpNotification(context, alarmId)
                
                // Schedule re-trigger in 2 minutes if not answered
                scheduleReTrigger(context, alarmId, 2 * 60 * 1000)
            }
            "CONFIRM_AWAKE" -> {
                Log.d("WakeUpReceiver", "User confirmed awake for $alarmId")
                NotificationHelper.dismissNotification(context)
                cancelReTrigger(context, alarmId)
            }
            "SAFETY_CHECK" -> {
                val delayMin = intent.getIntExtra("DELAY_MIN", 5)
                Log.d("WakeUpReceiver", "Safety check scheduled in $delayMin minutes for $alarmId")
                NotificationHelper.dismissNotification(context)
                cancelReTrigger(context, alarmId)
                scheduleWakeUpCheck(context, alarmId, delayMin * 60 * 1000L)
            }
            "RE_TRIGGER_ALARM" -> {

                Log.d("WakeUpReceiver", "Wake-up check failed. Re-triggering alarm $alarmId")
                NotificationHelper.dismissNotification(context)
                
                // Start the app to show missions
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("TRIGGER_ALARM_ID", alarmId)
                }
                context.startActivity(launchIntent)
            }
        }
    }

    private fun scheduleReTrigger(context: Context, alarmId: String, delayMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WakeUpReceiver::class.java).apply {
            action = "RE_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMillis,
            pendingIntent
        )
    }

    private fun cancelReTrigger(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WakeUpReceiver::class.java).apply {
            action = "RE_TRIGGER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        fun scheduleWakeUpCheck(context: Context, alarmId: String, delayMillis: Long = 5 * 60 * 1000) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, WakeUpReceiver::class.java).apply {
                action = "START_WAKE_UP_CHECK"
                putExtra("ALARM_ID", alarmId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, alarmId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent
            )
        }
    }
}

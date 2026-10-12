package com.carcosa.luminity

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "wake_up_check_channel"
    private const val CHANNEL_NAME = "Wake-up Check"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Verify if you are still awake after an alarm"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWakeUpNotification(context: Context, alarmId: String) {
        val confirmIntent = Intent(context, WakeUpReceiver::class.java).apply {
            action = "CONFIRM_AWAKE"
            putExtra("ALARM_ID", alarmId)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context, alarmId.hashCode() + 1, confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safety5Intent = Intent(context, WakeUpReceiver::class.java).apply {
            action = "SAFETY_CHECK"
            putExtra("ALARM_ID", alarmId)
            putExtra("DELAY_MIN", 5)
        }
        val safety5Pending = PendingIntent.getBroadcast(
            context, alarmId.hashCode() + 2, safety5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safety10Intent = Intent(context, WakeUpReceiver::class.java).apply {
            action = "SAFETY_CHECK"
            putExtra("ALARM_ID", alarmId)
            putExtra("DELAY_MIN", 10)
        }
        val safety10Pending = PendingIntent.getBroadcast(
            context, alarmId.hashCode() + 3, safety10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake-up Check!")
            .setContentText("Are you still awake?")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(R.drawable.ic_menu_view, "I'm Awake", confirmPending)
            .addAction(R.drawable.ic_menu_recent_history, "Safety Check (5m)", safety5Pending)
            .addAction(R.drawable.ic_menu_recent_history, "Safety Check (10m)", safety10Pending)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }


    fun dismissNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

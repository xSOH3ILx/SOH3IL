package com.soh3il.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object MidnightScheduler {

    const val ACTION_MIDNIGHT_TICK = "com.soh3il.calendar.ACTION_MIDNIGHT_TICK"
    private const val REQUEST_CODE_MIDNIGHT = 9999

    /**
     * Schedules the next exact midnight tick (00:00:01) to update the persistent notification,
     * widget, and launcher live icon when the date rolls over.
     */
    fun scheduleMidnightAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAtMillis = cal.timeInMillis

        val intent = Intent(context, MidnightReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}

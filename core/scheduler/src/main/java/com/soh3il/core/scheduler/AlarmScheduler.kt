package com.soh3il.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.Reminder

object AlarmScheduler {

    const val ACTION_ALARM_TRIGGER = "com.soh3il.calendar.ACTION_ALARM_TRIGGER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"

    /**
     * Schedules the next exact alarm using setAlarmClock (or setExactAndAllowWhileIdle)
     */
    fun scheduleReminder(context: Context, reminder: Reminder) {
        val nextTrigger = if (reminder.nextTriggerEpochMillis != null && reminder.nextTriggerEpochMillis > System.currentTimeMillis()) {
            reminder.nextTriggerEpochMillis
        } else {
            JalaliRecurrenceEngine.calculateNextTrigger(reminder)
        } ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Main Alarm Intent
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, context, nextTrigger, pendingIntent)

        // Schedule Pre-Reminders
        val preTriggers = JalaliRecurrenceEngine.calculatePreReminderTriggers(reminder.preReminders, nextTrigger)
        preTriggers.forEachIndexed { index, preEpoch ->
            if (preEpoch > System.currentTimeMillis()) {
                val preIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGER
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                    putExtra(EXTRA_IS_PRE_REMINDER, true)
                }
                val prePendingIntent = PendingIntent.getBroadcast(
                    context,
                    (reminder.id * 1000 + (index + 1)).toInt(),
                    preIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExact(alarmManager, context, preEpoch, prePendingIntent)
            }
        }
    }

    private fun scheduleExact(
        alarmManager: AlarmManager,
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
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
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

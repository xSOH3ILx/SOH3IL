package com.soh3il.core.scheduler

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.Reminder
import java.util.Objects

object AlarmScheduler {

    const val ACTION_ALARM_TRIGGER = "com.soh3il.calendar.ACTION_ALARM_TRIGGER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    const val EXTRA_PRE_OFFSET_MINUTES = "extra_pre_offset_minutes"

    /**
     * Deterministic composite request code and notification ID generator
     * to eliminate collisions between main reminders and pre-reminders.
     */
    fun computeAlarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int {
        return Objects.hash(reminderId, preOffsetMinutes)
    }

    /**
     * Alias matching AGENTS.md Rule 3.4
     */
    fun alarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int =
        computeAlarmRequestCode(reminderId, preOffsetMinutes)

    /**
     * Schedules a snooze alarm for a reminder (default: 10 minutes from now).
     */
    fun scheduleSnooze(context: Context, reminderId: Long, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, 0)
        }

        val requestCode = computeAlarmRequestCode(reminderId, 0)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Schedules the next exact alarm using setAlarmClock (or setExactAndAllowWhileIdle fallback).
     */
    fun scheduleReminder(context: Context, reminder: Reminder) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder)
            return
        }

        val nextTrigger = if (reminder.nextTriggerEpochMillis > System.currentTimeMillis()) {
            reminder.nextTriggerEpochMillis
        } else {
            JalaliRecurrenceEngine.calculateNextTrigger(reminder)
        } ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Main Alarm Intent (offset = 0)
        val mainRequestCode = computeAlarmRequestCode(reminder.id, 0)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mainRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, nextTrigger, pendingIntent)

        // 2. Schedule Pre-Reminders with unique composite request codes
        reminder.preReminders.forEach { preReminder ->
            val preMinutes = preReminder.toMinutes().toInt()
            val preEpoch = nextTrigger - (preMinutes * 60 * 1000L)
            if (preEpoch > System.currentTimeMillis()) {
                val preRequestCode = computeAlarmRequestCode(reminder.id, preMinutes)
                val preIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGER
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                    putExtra(EXTRA_IS_PRE_REMINDER, true)
                    putExtra(EXTRA_PRE_OFFSET_MINUTES, preMinutes)
                }
                val prePendingIntent = PendingIntent.getBroadcast(
                    context,
                    preRequestCode,
                    preIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExact(alarmManager, preEpoch, prePendingIntent)
            }
        }
    }

    private fun scheduleExact(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
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
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminder: Reminder) {
        val offsets = reminder.preReminders.map { it.toMinutes().toInt() } + 0
        cancelReminder(context, reminder.id, offsets)
    }

    fun cancelReminder(context: Context, reminderId: Long, preOffsetMinutesList: List<Int> = emptyList()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allOffsets = (preOffsetMinutesList + 0).distinct()
        for (offset in allOffsets) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_PRE_OFFSET_MINUTES, offset)
            }
            val requestCode = computeAlarmRequestCode(reminderId, offset)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            NotificationHelper.cancelReminderNotification(context, reminderId, offset)
        }

        // Also cancel base notification ID defensively
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(reminderId.toInt())
    }
}

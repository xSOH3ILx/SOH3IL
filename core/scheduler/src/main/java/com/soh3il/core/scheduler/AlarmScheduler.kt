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
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val EXTRA_PRE_OFFSET_MINUTES = "extra_pre_offset_minutes"

    /**
     * Negative discriminator so snooze never replaces the main alarm (offset 0)
     * or a pre-reminder (offset > 0).
     */
    const val SNOOZE_OFFSET = -1

    /**
     * Deterministic composite request code and notification ID generator
     * to eliminate collisions between main reminders and pre-reminders.
     */
    fun computeAlarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int {
        return Objects.hash(reminderId, preOffsetMinutes)
    }

    /**
     * Notification action PendingIntents must not share the alarm request-code space.
     */
    fun actionRequestCode(reminderId: Long, preOffsetMinutes: Int, actionSlot: Int): Int {
        return Objects.hash(reminderId, preOffsetMinutes, actionSlot)
    }

    /**
     * Alias matching AGENTS.md Rule 3.4
     */
    fun alarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int =
        computeAlarmRequestCode(reminderId, preOffsetMinutes)

    /**
     * Schedules a snooze alarm for a reminder (default: 10 minutes from now).
     * Does not replace the next real occurrence.
     */
    fun scheduleSnooze(context: Context, reminderId: Long, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_IS_SNOOZE, true)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, 0)
        }

        val requestCode = computeAlarmRequestCode(reminderId, SNOOZE_OFFSET)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Schedules the next exact alarm. Uses setExactAndAllowWhileIdle, never setAlarmClock,
     * so the status-bar clock cannot fire the reminder early.
     */
    fun scheduleReminder(
        context: Context,
        reminder: Reminder,
        forceRecalculate: Boolean = false
    ) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder)
            return
        }

        val nextTrigger = if (!forceRecalculate && reminder.nextTriggerEpochMillis > System.currentTimeMillis()) {
            reminder.nextTriggerEpochMillis
        } else {
            JalaliRecurrenceEngine.calculateNextTrigger(reminder)
        }
        cancelAlarms(context, reminder.id, reminder.preReminders.map { it.toMinutes().toInt() })
        if (nextTrigger == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val mainRequestCode = computeAlarmRequestCode(reminder.id, 0)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_IS_SNOOZE, false)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mainRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, nextTrigger, pendingIntent)

        reminder.preReminders.forEach { preReminder ->
            val preMinutes = preReminder.toMinutes().toInt()
            if (preMinutes <= 0) return@forEach
            val preEpoch = nextTrigger - (preMinutes * 60 * 1000L)
            if (preEpoch > System.currentTimeMillis()) {
                val preRequestCode = computeAlarmRequestCode(reminder.id, preMinutes)
                val preIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGER
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                    putExtra(EXTRA_IS_PRE_REMINDER, true)
                    putExtra(EXTRA_IS_SNOOZE, false)
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
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
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
        cancelAlarms(context, reminderId, preOffsetMinutesList)
        for (offset in (preOffsetMinutesList + 0).distinct()) {
            NotificationHelper.cancelReminderNotification(context, reminderId, offset)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(computeAlarmRequestCode(reminderId, 0))
        notificationManager?.cancel(computeAlarmRequestCode(reminderId, SNOOZE_OFFSET))
    }

    private fun cancelAlarms(context: Context, reminderId: Long, preOffsetMinutesList: List<Int>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allOffsets = (preOffsetMinutesList + 0 + SNOOZE_OFFSET).distinct()
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
        }
    }
}

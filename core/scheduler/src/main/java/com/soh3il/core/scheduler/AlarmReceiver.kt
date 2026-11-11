package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.RecurrenceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        // Strict whitelist for internal alarm and notification action triggers
        if (action != AlarmScheduler.ACTION_ALARM_TRIGGER &&
            action != NotificationHelper.ACTION_MARK_DONE &&
            action != NotificationHelper.ACTION_DISMISS_REMINDER &&
            action != NotificationHelper.ACTION_SNOOZE_REMINDER &&
            action != NotificationHelper.ACTION_SNOOZE_15
        ) {
            return
        }

        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_500L) {
                    val db = Soh3ilDatabase.getInstance(context)
                    val entity = db.reminderDao().getReminderById(reminderId)

                    when (action) {
                        AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                            val isPre = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
                            val preOffsetMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, 0)
                            if (entity != null && entity.isEnabled) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    reminderId = entity.id,
                                    title = entity.title,
                                    description = entity.description,
                                    isPreReminder = isPre,
                                    preOffsetMinutes = preOffsetMinutes
                                )

                                // If this was the main trigger, calculate and schedule the next recurrence!
                                if (!isPre) {
                                    val domain = entity.toDomain()
                                    if (domain.recurrence.type == RecurrenceType.NONE) {
                                        // Mark disabled if non-recurring
                                        db.reminderDao().updateReminder(entity.copy(isEnabled = false))
                                    } else {
                                        // Reschedule for next solar recurrence
                                        val nextEpoch = JalaliRecurrenceEngine.calculateNextTrigger(
                                            reminder = domain,
                                            referenceEpochMillis = System.currentTimeMillis() + 1000L
                                        )
                                        if (nextEpoch != null) {
                                            db.reminderDao().updateReminder(entity.copy(nextTriggerEpochMillis = nextEpoch))
                                            AlarmScheduler.scheduleReminder(context, domain.copy(nextTriggerEpochMillis = nextEpoch))
                                        }
                                    }
                                }
                            }
                        }

                        NotificationHelper.ACTION_MARK_DONE,
                        NotificationHelper.ACTION_DISMISS_REMINDER -> {
                            if (entity != null) {
                                val domain = entity.toDomain()
                                if (domain.recurrence.type == RecurrenceType.NONE) {
                                    db.reminderDao().updateReminder(entity.copy(isEnabled = false))
                                }
                                val preOffsetMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, 0)
                                NotificationHelper.cancelReminderNotification(context, reminderId, preOffsetMinutes)
                            }
                        }

                        NotificationHelper.ACTION_SNOOZE_REMINDER,
                        NotificationHelper.ACTION_SNOOZE_15 -> {
                            if (entity != null) {
                                val snoozeMinutes = intent.getIntExtra(
                                    NotificationHelper.EXTRA_SNOOZE_MINUTES,
                                    if (action == NotificationHelper.ACTION_SNOOZE_15) 15 else 10
                                )
                                AlarmScheduler.scheduleSnooze(context, reminderId, snoozeMinutes)
                                val preOffsetMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, 0)
                                NotificationHelper.cancelReminderNotification(context, reminderId, preOffsetMinutes)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

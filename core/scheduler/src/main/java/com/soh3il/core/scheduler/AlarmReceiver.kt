package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.RecurrenceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Soh3ilDatabase.getInstance(context)
                val entity = db.reminderDao().getReminderById(reminderId)

                when (intent.action) {
                    AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                        val isPre = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
                        if (entity != null && entity.isEnabled) {
                            NotificationHelper.showReminderNotification(
                                context = context,
                                reminderId = entity.id,
                                title = entity.title,
                                description = entity.description,
                                isPreReminder = isPre
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

                    NotificationHelper.ACTION_MARK_DONE -> {
                        if (entity != null) {
                            val domain = entity.toDomain()
                            if (domain.recurrence.type == RecurrenceType.NONE) {
                                db.reminderDao().updateReminder(entity.copy(isEnabled = false))
                            }
                        }
                    }

                    NotificationHelper.ACTION_SNOOZE_15 -> {
                        if (entity != null) {
                            val domain = entity.toDomain()
                            val snoozeTrigger = System.currentTimeMillis() + (15 * 60 * 1000L)
                            AlarmScheduler.scheduleReminder(
                                context,
                                domain.copy(nextTriggerEpochMillis = snoozeTrigger)
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

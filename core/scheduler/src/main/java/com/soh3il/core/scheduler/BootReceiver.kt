package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soh3il.core.database.Soh3ilDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all active reminders after phone restart, app update, or timezone change.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )

        if (intent.action !in validActions) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Soh3ilDatabase.getInstance(context)
                val activeReminders = db.reminderDao().getAllActiveRemindersSync()
                val domainList = activeReminders.map { it.toDomain() }

                domainList.forEach { reminder ->
                    AlarmScheduler.scheduleReminder(context, reminder)
                }

                PersistentNotificationHelper.updateNotification(context, domainList)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

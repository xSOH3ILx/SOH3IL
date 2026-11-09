package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for:
 * 1. Rolling over date at midnight (00:00:01) to update notification, widget, and live launcher icon.
 * 2. Immediately restoring persistent notification if user swipes it away (via deleteIntent).
 * 3. Handling system boot, package replacement, and time/timezone changes.
 */
class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Soh3ilDatabase.getInstance(context)
                val activeReminders = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }

                // 1. Notification Dismissed by User -> Auto-Restore if Enabled
                if (action == ACTION_NOTIFICATION_DISMISSED) {
                    if (UserPreferences.isPersistentNotificationEnabled(context)) {
                        PersistentNotificationHelper.updateNotification(context, activeReminders)
                    }
                    return@launch
                }

                // 2. Reschedule reminder alarms on device boot or app update
                if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    activeReminders.forEach { reminder ->
                        AlarmScheduler.scheduleReminder(context, reminder)
                    }
                }

                // 3. Update Persistent Status Bar Notification
                PersistentNotificationHelper.updateNotification(context, activeReminders)

                // 4. Update Home Screen Widget
                triggerWidgetUpdate(context)

                // 5. Update Home Screen Dynamic Live Icon
                LiveIconHelper.updateLiveIcon(context)

                // 6. Schedule Next Midnight Alarm
                MidnightScheduler.scheduleMidnightAlarm(context)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_NOTIFICATION_DISMISSED = "com.soh3il.calendar.ACTION_NOTIFICATION_DISMISSED"
    }
}

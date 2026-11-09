package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * BroadcastReceiver responsible for:
 * 1. Rolling over date at midnight (00:00:01) to update notification, widget, and live launcher icon.
 * 2. Immediately restoring persistent notification if user swipes it away (via deleteIntent).
 * 3. Handling system boot, package replacement, time/timezone changes, and exact alarm permission changes.
 */
class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        // Strict whitelist for system events and internal scheduled triggers
        val allowedActions = setOf(
            ACTION_MIDNIGHT_TICK,
            ACTION_NOTIFICATION_DISMISSED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in allowedActions) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_500L) {
                    val db = Soh3ilDatabase.getInstance(context)
                    val activeReminders = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }

                    // 1. Notification Dismissed by User -> Auto-Restore if Enabled
                    if (action == ACTION_NOTIFICATION_DISMISSED) {
                        if (UserPreferences.isPersistentNotificationEnabled(context)) {
                            PersistentNotificationHelper.updateNotification(context, activeReminders)
                        }
                        return@withTimeoutOrNull
                    }

                    // 2. Reschedule reminder alarms on device boot, app update, time changes, or exact alarm permission grant
                    if (action == Intent.ACTION_BOOT_COMPLETED ||
                        action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                        action == Intent.ACTION_TIME_CHANGED ||
                        action == Intent.ACTION_TIMEZONE_CHANGED ||
                        action == ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
                    ) {
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
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE").apply {
                component = ComponentName(
                    context.packageName,
                    "com.soh3il.calendar.widget.CalendarAppWidgetProvider"
                )
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_MIDNIGHT_TICK = "com.soh3il.calendar.ACTION_MIDNIGHT_TICK"
        const val ACTION_NOTIFICATION_DISMISSED = "com.soh3il.calendar.ACTION_NOTIFICATION_DISMISSED"
        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}

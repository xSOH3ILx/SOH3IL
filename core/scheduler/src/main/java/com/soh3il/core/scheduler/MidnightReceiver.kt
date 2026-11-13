package com.soh3il.core.scheduler

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles in-app midnight and notification-dismiss signals.
 * System broadcasts are accepted only by [SystemEventReceiver], which delegates here.
 */
class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in INTERNAL_ACTIONS) return
        enqueueFromReceiver(this, context, action)
    }

    companion object {
        private const val TAG = "MidnightReceiver"
        const val ACTION_MIDNIGHT_TICK = "com.soh3il.calendar.ACTION_MIDNIGHT_TICK"
        const val ACTION_NOTIFICATION_DISMISSED = "com.soh3il.calendar.ACTION_NOTIFICATION_DISMISSED"
        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

        private val INTERNAL_ACTIONS = setOf(
            ACTION_MIDNIGHT_TICK,
            ACTION_NOTIFICATION_DISMISSED
        )

        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON
        )

        fun enqueueFromReceiver(receiver: BroadcastReceiver, context: Context, action: String) {
            val pendingResult = receiver.goAsync()
            refresh(context.applicationContext, action, pendingResult)
        }

        private fun refresh(
            context: Context,
            action: String,
            pendingResult: BroadcastReceiver.PendingResult?
        ) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    withTimeoutOrNull(8_500L) {
                        val db = Soh3ilDatabase.getInstance(context)
                        val activeReminders = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }

                        if (action == ACTION_NOTIFICATION_DISMISSED) {
                            if (UserPreferences.isPersistentNotificationEnabled(context) &&
                                NotificationHelper.canPostNotifications(context)
                            ) {
                                PersistentNotificationHelper.updateNotification(context, activeReminders)
                            }
                            return@withTimeoutOrNull
                        }

                        if (action in RESCHEDULE_ACTIONS) {
                            activeReminders.forEach { reminder ->
                                AlarmScheduler.scheduleReminder(
                                    context = context,
                                    reminder = reminder,
                                    forceRecalculate = true
                                )
                            }
                        }

                        PersistentNotificationHelper.updateNotification(context, activeReminders)
                        requestWidgetUpdate(context)
                        LiveIconHelper.updateLiveIcon(context)
                        MidnightScheduler.scheduleMidnightAlarm(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "calendar refresh failed: ${e.javaClass.simpleName}")
                } finally {
                    pendingResult?.finish()
                }
            }
        }

        fun requestWidgetUpdate(context: Context) {
            try {
                val component = ComponentName(
                    context.packageName,
                    "com.soh3il.calendar.widget.CalendarAppWidgetProvider"
                )
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isEmpty()) return
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    this.component = component
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "widget refresh failed: ${e.javaClass.simpleName}")
            }
        }
    }
}

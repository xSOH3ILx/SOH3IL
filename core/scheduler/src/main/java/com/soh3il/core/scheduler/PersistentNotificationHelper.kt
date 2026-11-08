package com.soh3il.core.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder

object PersistentNotificationHelper {

    private const val CHANNEL_ID = "persistent_date_channel"
    private const val CHANNEL_NAME = "نوار اعلان دائمی تاریخ"
    private const val NOTIFICATION_ID = 2001
    const val EXTRA_OPEN_ADD_REMINDER = "extra_open_add_reminder"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش پیوسته تاریخ روز شمسی و یادآورهای جاری در نوار وضعیت"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(context: Context, reminders: List<Reminder>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!UserPreferences.isPersistentNotificationEnabled(context)) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        createNotificationChannel(context)

        val today = JalaliDate.now()
        val title = "${today.dayOfWeekNameFa}، ${today.day.toPersianDigits()} ${today.monthNameFa} ${today.year.toPersianDigits()}"

        // Occasions for today
        val todayStr = today.format("YYYY-MM-DD")
        val occasions = IranianHolidays.getOccasionsForMonth(today.year, today.month)
            .filter { it.jalaliDate == todayStr }

        // Today's active reminders count
        val todayRemindersCount = reminders.count {
            it.jalaliYear == today.year &&
                    it.jalaliMonth == today.month &&
                    it.jalaliDay == today.day &&
                    it.isEnabled
        }

        val parts = mutableListOf<String>()
        if (occasions.isNotEmpty()) {
            parts.add(occasions.joinToString("، ") { it.titleFa })
        }
        if (todayRemindersCount > 0) {
            parts.add("${todayRemindersCount.toPersianDigits()} یادآور برای امروز")
        } else {
            parts.add("بدون یادآور برای امروز")
        }
        val contentText = parts.joinToString(" • ")

        // Content intent (opens MainActivity)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intent for "+ یادآور جدید" only (as requested by user, no other buttons)
        val addReminderIntent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
            putExtra(EXTRA_OPEN_ADD_REMINDER, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addReminderPendingIntent = PendingIntent.getActivity(
            context,
            1,
            addReminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_menu_today

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "افزودن یادآور", addReminderPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

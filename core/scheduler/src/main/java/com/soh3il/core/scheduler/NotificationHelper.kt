package com.soh3il.core.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.soh3il.core.database.preferences.UserPreferences

object NotificationHelper {

    const val CHANNEL_ID = "soh3il_reminders_channel"
    const val CHANNEL_NAME = "یادآورهای تقویم شمسی"

    const val ACTION_MARK_DONE = "com.soh3il.calendar.ACTION_MARK_DONE"
    const val ACTION_DISMISS_REMINDER = "com.soh3il.calendar.ACTION_DISMISS_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.soh3il.calendar.ACTION_SNOOZE_REMINDER"
    const val ACTION_SNOOZE_15 = "com.soh3il.calendar.ACTION_SNOOZE_15"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    private fun getEffectiveSoundUri(context: Context): Uri {
        val saved = UserPreferences.getNotificationSoundUri(context)
        return if (!saved.isNullOrEmpty()) {
            Uri.parse(saved)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun createNotificationChannel(context: Context, customSoundUri: Uri? = null) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val soundUri = customSoundUri ?: getEffectiveSoundUri(context)

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان‌های زنده و فوری یادآورهای تقویم SOH3IL"
            enableVibration(true)
            setSound(soundUri, audioAttributes)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateNotificationChannelSound(context: Context, newSoundUri: Uri?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Existing channels ignore sound changes. Recreate so the chosen ringtone applies.
        manager.deleteNotificationChannel(CHANNEL_ID)
        createNotificationChannel(context, newSoundUri)
    }

    fun cancelReminderNotification(context: Context, reminderId: Long, preOffsetMinutes: Int = 0) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = AlarmScheduler.computeAlarmRequestCode(reminderId, preOffsetMinutes)
        notificationManager.cancel(notificationId)
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        isPreReminder: Boolean = false,
        preOffsetMinutes: Int = 0
    ): Boolean {
        if (!canPostNotifications(context)) return false
        val soundUri = getEffectiveSoundUri(context)
        createNotificationChannel(context, soundUri)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = AlarmScheduler.computeAlarmRequestCode(reminderId, preOffsetMinutes)

        // Explicit Intent to launch MainActivity
        val launchIntent = Intent().setClassName(
            context,
            "com.soh3il.calendar.MainActivity"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Dismiss / Mark Done
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, preOffsetMinutes)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.actionRequestCode(reminderId, preOffsetMinutes, 1),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10 minutes
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
            putExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, preOffsetMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.actionRequestCode(reminderId, preOffsetMinutes, 2),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = if (isPreReminder) "پیش‌یادآور: $title" else title

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(displayTitle)
            .setContentText(description.ifEmpty { "زمان سررسید این رویداد فرا رسیده است." })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "انجام شد", donePendingIntent)
            .addAction(0, "تعویق ۱۰ دقیقه‌ای", snoozePendingIntent)

        return try {
            notificationManager.notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

package com.soh3il.core.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.soh3il.core.database.preferences.UserPreferences

object NotificationHelper {

    const val CHANNEL_ID = "soh3il_reminders_channel"
    const val CHANNEL_NAME = "یادآورهای تقویم شمسی"

    const val ACTION_MARK_DONE = "com.soh3il.calendar.ACTION_MARK_DONE"
    const val ACTION_SNOOZE_15 = "com.soh3il.calendar.ACTION_SNOOZE_15"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    private fun getEffectiveSoundUri(context: Context): Uri {
        val saved = UserPreferences.getNotificationSoundUri(context)
        return if (!saved.isNullOrEmpty()) {
            Uri.parse(saved)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun createNotificationChannel(context: Context, customSoundUri: Uri? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                description = "اعلان‌های زنده و فوری یادآورهای تقویم سُهیل"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun updateNotificationChannelSound(context: Context, newSoundUri: Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = newSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های زنده و فوری یادآورهای تقویم سُهیل"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        isPreReminder: Boolean = false
    ) {
        val soundUri = getEffectiveSoundUri(context)
        createNotificationChannel(context, soundUri)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch the main app
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Done
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 15 minutes
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_15
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 2).toInt(),
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
            .addAction(0, "تعویق ۱۵ دقیقه", snoozePendingIntent)

        notificationManager.notify(reminderId.toInt(), builder.build())
    }
}

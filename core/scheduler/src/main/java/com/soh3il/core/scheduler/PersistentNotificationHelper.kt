package com.soh3il.core.scheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder

object PersistentNotificationHelper {

    private const val CHANNEL_ID = "persistent_date_channel"
    private const val CHANNEL_NAME = "نوار اعلان دائمی تاریخ"
    private const val NOTIFICATION_ID = 2001
    const val EXTRA_OPEN_ADD_REMINDER = "extra_open_add_reminder"

    private val GREGORIAN_MONTHS_FA = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    private val HIJRI_MONTHS_FA = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی",
        "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان",
        "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش پیوسته تاریخ روز شمسی و رویدادها در نوار وضعیت، صفحه قفل و AOD"
                setShowBadge(true)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createDayNumberBitmap(context: Context, dayNumber: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (32 * density).toInt().coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val text = dayNumber.toPersianDigits()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (text.length > 2) sizePx * 0.48f else sizePx * 0.68f
            textAlign = Paint.Align.CENTER
        }

        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)
        val x = sizePx / 2f
        val y = (sizePx / 2f) - bounds.exactCenterY()

        canvas.drawText(text, x, y, textPaint)
        return bitmap
    }

    private fun getAppIconBitmap(context: Context): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Throwable) {
            null
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

        // Gregorian & Hijri equivalent
        val conv = DateConverter.fromJalali(today.year, today.month, today.day)
        val gregStr = "${conv.gregorian.dayOfMonth.toPersianDigits()} ${GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }} ${conv.gregorian.year.toPersianDigits()}"
        val hijriStr = if (conv.hijriYear > 0) {
            "${conv.hijriDay.toPersianDigits()} ${HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }} ${conv.hijriYear.toPersianDigits()}"
        } else null
        val dateSummary = if (hijriStr != null) "$gregStr • $hijriStr" else gregStr

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

        // Action intent for "+ یادآور جدید"
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(dateSummary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$dateSummary\n$contentText")
                    .setBigContentTitle(title)
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "افزودن یادآور", addReminderPendingIntent)

        // Set dynamic day number as smallIcon in status bar
        try {
            val dayBitmap = createDayNumberBitmap(context, today.day)
            builder.setSmallIcon(IconCompat.createWithBitmap(dayBitmap))
        } catch (_: Throwable) {
            val fallbackIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_menu_today
            builder.setSmallIcon(fallbackIcon)
        }

        // Set app icon as largeIcon in notification body
        val appIcon = getAppIconBitmap(context)
        if (appIcon != null) {
            builder.setLargeIcon(appIcon)
        }

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

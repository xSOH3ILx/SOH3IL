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
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.matchesDate
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PersistentNotificationHelper {

    private const val CHANNEL_ID = "persistent_date_channel"
    private const val CHANNEL_NAME = "نوار اعلان دائمی تاریخ"
    private const val NOTIFICATION_ID = 2001
    const val EXTRA_OPEN_ADD_REMINDER = "extra_open_add_reminder"
    const val ACTION_OPEN_ADD_REMINDER = "com.soh3il.calendar.action.OPEN_ADD_REMINDER"

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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
                description = "نمایش پیوسته تاریخ روز خورشیدی و رویدادها در نوار وضعیت، صفحه قفل و AOD"
            setShowBadge(true)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createDayNumberBitmap(context: Context, dayNumber: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (32 * density).toInt().coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val text = dayNumber.toPersianDigits()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 255, 74)
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

    /**
     * بَدج اختصاصی تاریخ امروز با سبک تقویم متریال ۳ جهت نمایش به عنوان LargeIcon اعلان
     */
    private fun createDateBadgeBitmap(context: Context, today: JalaliDate): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (72 * density).toInt().coerceAtLeast(144)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = sizePx * 0.24f
        val fullRect = android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())

        // پس‌زمینه خنثی آیکون و اعلان (خاکستری Expressive)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(91, 98, 106)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(fullRect, cornerRadius, cornerRadius, bgPaint)

        // نوار بالایی نام ماه با سبز فسفری خوانا
        val headerHeight = sizePx * 0.34f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 255, 74)
            style = Paint.Style.FILL
        }
        val headerPath = android.graphics.Path().apply {
            addRoundRect(
                fullRect,
                floatArrayOf(
                    cornerRadius, cornerRadius,
                    cornerRadius, cornerRadius,
                    0f, 0f,
                    0f, 0f
                ),
                android.graphics.Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipRect(0f, 0f, sizePx.toFloat(), headerHeight)
        canvas.drawPath(headerPath, headerPaint)
        canvas.restore()

        // متن نام ماه خورشیدی
        val monthText = today.monthNameFa
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 39, 43)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = headerHeight * 0.52f
            textAlign = Paint.Align.CENTER
        }
        val monthBounds = Rect()
        monthPaint.getTextBounds(monthText, 0, monthText.length, monthBounds)
        val monthY = (headerHeight / 2f) - monthBounds.exactCenterY()
        canvas.drawText(monthText, sizePx / 2f, monthY, monthPaint)

        // عدد بزرگ روز خورشیدی در بدنه کارت
        val dayText = today.day.toPersianDigits()
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 255, 74)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = (sizePx - headerHeight) * 0.65f
            textAlign = Paint.Align.CENTER
        }
        val dayBounds = Rect()
        dayPaint.getTextBounds(dayText, 0, dayText.length, dayBounds)
        val bodyCenterY = headerHeight + (sizePx - headerHeight) / 2f
        val dayY = bodyCenterY - dayBounds.exactCenterY()
        canvas.drawText(dayText, sizePx / 2f, dayY, dayPaint)

        return bitmap
    }

    @Suppress("DiscouragedApi")
    private fun getDayDrawableRes(context: Context, day: Int): Int {
        val safeDay = day.coerceIn(1, 31)
        val resName = "ic_stat_day_$safeDay"
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) return resId

        val dsResId = context.resources.getIdentifier(resName, "drawable", "com.soh3il.core.designsystem")
        if (dsResId != 0) return dsResId

        return android.R.drawable.ic_menu_today
    }

    fun updateNotification(context: Context, reminders: List<Reminder>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!UserPreferences.isPersistentNotificationEnabled(context)) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }
        if (!NotificationHelper.canPostNotifications(context)) {
            return
        }

        createNotificationChannel(context)

        val today = JalaliDate.now()
        val rlm = "\u200F" // نویسه اجباری راست‌چین یونیکد (Right-To-Left Mark)

        val fullTitle = "$rlm${today.dayOfWeekNameFa}، ${today.day.toPersianDigits()} ${today.monthNameFa} ${today.year.toPersianDigits()}"

        // Gregorian & Hijri equivalent
        val conv = DateConverter.fromJalali(today.year, today.month, today.day)
        val gregStr = "${conv.gregorian.dayOfMonth.toPersianDigits()} ${GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }} ${conv.gregorian.year.toPersianDigits()}"
        val hijriStr = if (conv.hijriYear > 0) {
            "${conv.hijriDay.toPersianDigits()} ${HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }} ${conv.hijriYear.toPersianDigits()}"
        } else null

        // Occasions for today
        val todayStr = today.format("YYYY-MM-DD")
        val occasions = IranianHolidays.getOccasionsForMonth(today.year, today.month)
            .filter { it.jalaliDate == todayStr }

        // Today's active reminders count (including recurring reminders)
        val todayRemindersCount = reminders.count { it.matchesDate(today) }

        // تفکیک خطوط به شکل ساختاریافته، کاملاً راست‌چین و بدون فشردگی خطی (بدون ایموجی طبق قوانین طراحی برنامه)
        val lineJalali = "${rlm}تقویم خورشیدی: ${today.dayOfWeekNameFa}، ${today.day.toPersianDigits()} ${today.monthNameFa} ${today.year.toPersianDigits()}"
        val showGregorian = UserPreferences.isPersistentGregorianEnabled(context)
        val showHijri = UserPreferences.isPersistentHijriEnabled(context)
        val lineGregorian = if (showGregorian) {
            "${rlm}تقویم میلادی: $gregStr (${conv.gregorian.dayOfMonth.toPersianDigits()} ${conv.dayOfWeekNameEn} ${conv.gregorian.year.toPersianDigits()})"
        } else null
        val lineHijri = if (showHijri && hijriStr != null) {
            "${rlm}تقویم هجری قمری محاسباتی: $hijriStr"
        } else null

        val lineOccasions = if (occasions.isNotEmpty()) {
            "${rlm}رویدادها: " + occasions.joinToString("، ") { it.titleFa }
        } else {
            "${rlm}رویدادها: بدون رویداد تعطیل"
        }

        val lineReminders = if (todayRemindersCount > 0) {
            "${rlm}یادآورها: ${todayRemindersCount.toPersianDigits()} یادآور برای امروز"
        } else {
            "${rlm}یادآورها: بدون یادآور برای امروز"
        }

        val notificationLines = listOfNotNull(
            lineJalali,
            lineGregorian,
            lineHijri,
            lineOccasions,
            lineReminders
        )

        // خلاصه فشرده جهت نمایش در حالت تک‌خطی اعلان (کاملاً راست‌چین با RLM)
        val dateParts = buildList {
            add("${today.day.toPersianDigits()} ${today.monthNameFa}")
            if (showGregorian) {
                add("${conv.gregorian.dayOfMonth.toPersianDigits()} ${GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }}")
            }
            if (showHijri && conv.hijriYear > 0) {
                add("${conv.hijriDay.toPersianDigits()} ${HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }}")
            }
        }
        val shortSummary = rlm + dateParts.joinToString(" • ")

        // Content intent (explicitly opens MainActivity)
        val launchIntent = Intent().setClassName(
            context,
            "com.soh3il.calendar.MainActivity"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intent for "+ یادآور جدید" (explicitly opens MainActivity)
        val addReminderIntent = Intent().setClassName(
            context,
            "com.soh3il.calendar.MainActivity"
        ).apply {
            action = ACTION_OPEN_ADD_REMINDER
            putExtra(EXTRA_OPEN_ADD_REMINDER, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addReminderPendingIntent = PendingIntent.getActivity(
            context,
            1,
            addReminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete intent: triggers automatic restoration if swiped away while enabled
        val deleteIntent = Intent(context, MidnightReceiver::class.java).apply {
            action = MidnightReceiver.ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = getDayDrawableRes(context, today.day)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(fullTitle)
            .setContentText(shortSummary)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("${rlm}تقویم خورشیدی SOH3IL")
                    .setBigContentTitle(fullTitle)
                    .also { style -> notificationLines.forEach(style::addLine) }
            )
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(
                if (UserPreferences.isLockscreenCalendarEnabled(context)) {
                    NotificationCompat.VISIBILITY_PUBLIC
                } else {
                    NotificationCompat.VISIBILITY_PRIVATE
                }
            )
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .addAction(android.R.drawable.ic_input_add, "افزودن یادآور", addReminderPendingIntent)

        // تنظیم تضمینی آیکون روز در نوار وضعیت با شناسه منبع استاندارد اندروید
        builder.setSmallIcon(smallIconRes)

        // نمایش بَدج گرافیکی تقویم امروز بجای آیکون پیش‌فرض برنامه در بدنه اعلان
        val dateBadgeBitmap = createDateBadgeBitmap(context, today)
        builder.setLargeIcon(dateBadgeBitmap)

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(fullTitle)
            .setContentText(shortSummary)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
        builder.setPublicVersion(publicVersion)

        val notification = builder.build()
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            return
        }

        // Ensure next midnight alarm is scheduled for exact date rollover
        MidnightScheduler.scheduleMidnightAlarm(context)
    }

    fun updateNotification(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = com.soh3il.core.database.Soh3ilDatabase.getInstance(context)
                val activeReminders = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
                updateNotification(context, activeReminders)
            } catch (_: Exception) {
                updateNotification(context, emptyList())
            }
        }
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

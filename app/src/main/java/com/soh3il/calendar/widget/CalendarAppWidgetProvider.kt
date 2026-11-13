package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.soh3il.calendar.R
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.matchesDate
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

class CalendarAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_500L) {
                    val db = Soh3ilDatabase.getInstance(context)
                    val activeReminders = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, activeReminders)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val GREGORIAN_MONTHS_FA = listOf(
            "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
            "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
        )

        private val HIJRI_MONTHS_FA = listOf(
            "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی",
            "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان",
            "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
        )

        fun requestUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, CalendarAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (allWidgetIds != null && allWidgetIds.isNotEmpty()) {
                    val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = thisWidget
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
                    }
                    context.sendBroadcast(updateIntent)
                }
            } catch (_: Exception) {}
        }

        fun updateAllWidgets(context: Context, reminders: List<Reminder>? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CalendarAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId, reminders)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            cachedReminders: List<Reminder>? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_calendar_m3)
            val today = JalaliDate.now()

            // 1. Day number and names
            views.setTextViewText(R.id.widget_day_number, today.day.toPersianDigits())
            views.setTextViewText(R.id.widget_day_of_week, today.dayOfWeekNameFa)
            views.setTextViewText(R.id.widget_month_name, today.monthNameFa)
            views.setTextViewText(R.id.widget_year_title, "${today.year.toPersianDigits()} خورشیدی")

            // 2. Gregorian and Hijri dates
            val conv = DateConverter.fromJalali(today.year, today.month, today.day)
            val gregShort = "${conv.gregorian.dayOfMonth.toPersianDigits()} ${GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }}"
            val hijriShort = if (conv.hijriYear > 0) {
                "${conv.hijriDay.toPersianDigits()} ${HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }}"
            } else null
            val otherDatesText = if (hijriShort != null) "$gregShort • $hijriShort" else gregShort
            views.setTextViewText(R.id.widget_other_dates, otherDatesText)

            // 3. Occasions of the day
            val todayStr = today.format("YYYY-MM-DD")
            val occasions = IranianHolidays.getOccasionsForMonth(today.year, today.month)
                .filter { it.jalaliDate == todayStr }

            if (occasions.isNotEmpty()) {
                val occTitle = occasions.joinToString("، ") { it.titleFa }
                views.setTextViewText(R.id.widget_occasion, occTitle)
            } else {
                views.setTextViewText(R.id.widget_occasion, "بدون مناسبت تعطیل")
            }

            // 4. Click opens MainActivity
            val launchIntent = Intent(context, com.soh3il.calendar.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 5. Build Reminders Summary with next upcoming reminder
            fun applyReminderSummary(activeList: List<Reminder>) {
                val todayReminders = activeList.filter { it.matchesDate(today) }
                    .sortedWith(compareBy({ it.hour }, { it.minute }))

                val nowCal = Calendar.getInstance()
                val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

                val summaryText = if (todayReminders.isNotEmpty()) {
                    val upcoming = todayReminders.firstOrNull { (it.hour * 60 + it.minute) >= currentMinutes }
                        ?: todayReminders.first()
                    val timeStr = "\u200E${upcoming.hour.toPersianDigits().padStart(2, '۰')}:${upcoming.minute.toPersianDigits().padStart(2, '۰')}"
                    "یادآور بعدی: ${upcoming.title} (ساعت $timeStr)"
                } else {
                    "بدون یادآور برای امروز"
                }

                views.setTextViewText(R.id.widget_reminders_summary, summaryText)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            if (cachedReminders != null) {
                applyReminderSummary(cachedReminders)
            } else {
                // Immediate display
                appWidgetManager.updateAppWidget(appWidgetId, views)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = Soh3ilDatabase.getInstance(context)
                        val active = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
                        applyReminderSummary(active)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
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

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CalendarAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
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
                views.setTextViewText(R.id.widget_occasion, "📌 $occTitle")
            } else {
                views.setTextViewText(R.id.widget_occasion, "📌 بدون مناسبت تعطیل")
            }

            // 4. Click opens MainActivity
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            // Immediate initial update
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // 5. Query active reminders asynchronously for count
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = Soh3ilDatabase.getInstance(context)
                    val reminderEntities = db.reminderDao().getAll()
                    val count = reminderEntities.count {
                        it.jalaliYear == today.year &&
                                it.jalaliMonth == today.month &&
                                it.jalaliDay == today.day &&
                                it.isEnabled
                    }
                    val reminderText = if (count > 0) {
                        "⏰ ${count.toPersianDigits()} یادآور فعال برای امروز"
                    } else {
                        "⏰ بدون یادآور برای امروز"
                    }
                    views.setTextViewText(R.id.widget_reminders_summary, reminderText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {}
            }
        }
    }
}

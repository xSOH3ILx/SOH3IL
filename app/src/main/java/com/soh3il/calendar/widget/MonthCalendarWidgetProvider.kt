package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.soh3il.calendar.MainActivity
import com.soh3il.calendar.R
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate

class MonthCalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    companion object {
        private val WEEKDAY_IDS = intArrayOf(
            R.id.month_weekday_0, R.id.month_weekday_1, R.id.month_weekday_2,
            R.id.month_weekday_3, R.id.month_weekday_4, R.id.month_weekday_5, R.id.month_weekday_6
        )
        private val CELL_IDS = intArrayOf(
            R.id.month_cell_0, R.id.month_cell_1, R.id.month_cell_2, R.id.month_cell_3, R.id.month_cell_4, R.id.month_cell_5, R.id.month_cell_6,
            R.id.month_cell_7, R.id.month_cell_8, R.id.month_cell_9, R.id.month_cell_10, R.id.month_cell_11, R.id.month_cell_12, R.id.month_cell_13,
            R.id.month_cell_14, R.id.month_cell_15, R.id.month_cell_16, R.id.month_cell_17, R.id.month_cell_18, R.id.month_cell_19, R.id.month_cell_20,
            R.id.month_cell_21, R.id.month_cell_22, R.id.month_cell_23, R.id.month_cell_24, R.id.month_cell_25, R.id.month_cell_26, R.id.month_cell_27,
            R.id.month_cell_28, R.id.month_cell_29, R.id.month_cell_30, R.id.month_cell_31, R.id.month_cell_32, R.id.month_cell_33, R.id.month_cell_34,
            R.id.month_cell_35, R.id.month_cell_36, R.id.month_cell_37, R.id.month_cell_38, R.id.month_cell_39, R.id.month_cell_40, R.id.month_cell_41
        )
        private val WEEKDAYS = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MonthCalendarWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateAppWidget(context, manager, it) }
        }

        private fun updateAppWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            manager.updateAppWidget(appWidgetId, createRemoteViews(context, appWidgetId))
        }

        internal fun createRemoteViews(context: Context, appWidgetId: Int, today: JalaliDate = JalaliDate.now()): RemoteViews {
            val firstDay = JalaliDate(today.year, today.month, 1)
            val holidays = IranianHolidays.getOccasionsForMonth(today.year, today.month)
                .filter { it.isHoliday }
                .map { it.jalaliDate }
                .toSet()
            val views = RemoteViews(context.packageName, R.layout.widget_calendar_month).apply {
                setTextViewText(R.id.month_title, "${today.monthNameFa} ${today.year.toPersianDigits()}")
                WEEKDAY_IDS.forEachIndexed { index, id -> setTextViewText(id, WEEKDAYS[index]) }
                CELL_IDS.forEachIndexed { index, id ->
                    val day = index - (firstDay.dayOfWeek - 1) + 1
                    if (day !in 1..today.daysInMonth) {
                        setTextViewText(id, "")
                        setInt(id, "setBackgroundResource", R.drawable.widget_month_cell_empty)
                    } else {
                        val date = JalaliDate(today.year, today.month, day)
                        setTextViewText(id, day.toPersianDigits())
                        val background = when {
                            date == today -> R.drawable.widget_month_cell_today
                            date.format("YYYY-MM-DD") in holidays || date.dayOfWeek == 7 -> R.drawable.widget_month_cell_holiday
                            else -> R.drawable.widget_month_cell_empty
                        }
                        setInt(id, "setBackgroundResource", background)
                        setTextColor(
                            id,
                            if (date == today) {
                                context.getColor(R.color.widget_badge_surface)
                            } else {
                                context.getColor(R.color.widget_on_surface)
                            }
                        )
                    }
                }
                setOnClickPendingIntent(
                    R.id.month_widget_root,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            return views
        }
    }
}

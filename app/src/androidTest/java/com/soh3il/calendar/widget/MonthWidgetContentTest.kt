package com.soh3il.calendar.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.soh3il.calendar.R
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthWidgetContentTest {
    @Test fun ordinaryOccasionDoesNotBecomeAHoliday() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val today = JalaliDate(1405, 8, 23)
        val ordinary = JalaliDate(1405, 8, 7)
        val occasions = IranianHolidays.getOccasionsForMonth(today.year, today.month)
            .filter { it.jalaliDate == ordinary.format("YYYY-MM-DD") }
        assertTrue(occasions.isNotEmpty())
        assertTrue(occasions.none { it.isHoliday })
        assertFalse(ordinary.dayOfWeek == 7)
        val root = MonthCalendarWidgetProvider.createRemoteViews(context, 123, today)
            .apply(context, FrameLayout(context))
        fun cell(date: JalaliDate): TextView {
            val index = JalaliDate(date.year, date.month, 1).dayOfWeek - 1 + date.day - 1
            return root.findViewById(context.resources.getIdentifier("month_cell_$index", "id", context.packageName))
        }
        assertEquals("۷", cell(ordinary).text.toString())
        assertEquals(Color.TRANSPARENT, (cell(ordinary).background as GradientDrawable).color!!.defaultColor)
        val friday = (1..today.daysInMonth).map { JalaliDate(today.year, today.month, it) }.first { it.dayOfWeek == 7 }
        assertEquals(context.getColor(R.color.widget_holiday_surface), (cell(friday).background as GradientDrawable).color!!.defaultColor)
    }
}

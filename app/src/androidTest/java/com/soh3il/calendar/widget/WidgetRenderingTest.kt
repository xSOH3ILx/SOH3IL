package com.soh3il.calendar.widget

import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.soh3il.calendar.R
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRenderingTest {
    private fun assertMonthGridFits(widthDp: Int, heightDp: Int, fontScale: Float) {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = Configuration(app.resources.configuration).apply { this.fontScale = fontScale }
        val context = app.createConfigurationContext(config)
        val root = RemoteViews(context.packageName, R.layout.widget_calendar_month).apply(context, FrameLayout(context))
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (heightDp * density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        for (index in 0..6) {
            val id = context.resources.getIdentifier("month_weekday_$index", "id", context.packageName)
            val weekday = root.findViewById<View>(id)
            assertTrue("Weekday $index has no width", weekday.width > 0)
        }
        for (index in 0..41) {
            val id = context.resources.getIdentifier("month_cell_$index", "id", context.packageName)
            val cell = root.findViewById<View>(id)
            val bounds = android.graphics.Rect()
            cell.getDrawingRect(bounds)
            (root as android.view.ViewGroup).offsetDescendantRectToMyCoords(cell, bounds)
            assertTrue("Cell $index is empty", cell.width > 0 && cell.height > 0)
            assertTrue("Cell $index overflows", bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width && bounds.bottom <= height)
            val text = cell as android.widget.TextView
            if (text.text.isNotEmpty()) {
                assertTrue("Cell $index clips: layout=${text.layout.height}, height=${text.height}, size=${text.textSize}, relayout=${root.isLayoutRequested}", text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom)
            }
        }
    }

    @Test fun compactMonthGridFits() = assertMonthGridFits(250, 180, 1f)
    @Test fun expandedMonthGridFits() = assertMonthGridFits(400, 320, 1f)
    @Test fun compactMonthGridFitsAtLargeFont() = assertMonthGridFits(250, 180, 1.5f)
    @Test fun expandedMonthGridFitsAtLargeFont() = assertMonthGridFits(400, 320, 1.5f)
}

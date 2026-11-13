package com.soh3il.calendar.integrations

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

object CalendarProviderSync {
    suspend fun syncReminders(context: Context, reminders: List<Reminder>): Result<Int> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext Result.failure(SecurityException("مجوز تقویم صادر نشده است"))
        }

        val calendarId = findWritableCalendarId(context) ?:
            return@withContext Result.failure(IllegalStateException("تقویم قابل ویرایش پیدا نشد"))
        var synced = 0
        reminders.filter { it.id != 0L && it.isEnabled }.forEach { reminder ->
            val start = JalaliDate(
                reminder.jalaliYear,
                reminder.jalaliMonth,
                reminder.jalaliDay
            ).toEpochMillis(reminder.hour, reminder.minute, 0, JalaliDate.TEHRAN_ZONE)
            val marker = "[SOH3IL_REMINDER:${reminder.id}]"
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, reminder.title)
                put(CalendarContract.Events.DESCRIPTION, "${reminder.description}\n$marker")
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, start + 60 * 60 * 1000L)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val existingId = findEventId(context, marker)
            val uri = if (existingId != null) {
                context.contentResolver.update(
                    CalendarContract.Events.CONTENT_URI,
                    values,
                    "${CalendarContract.Events._ID} = ?",
                    arrayOf(existingId.toString())
                )
                CalendarContract.Events.CONTENT_URI.buildUpon().appendPath(existingId.toString()).build()
            } else {
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
            if (uri != null) synced++
        }
        Result.success(synced)
    }

    private fun findWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars._ID} ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    private fun findEventId(context: Context, marker: String): Long? {
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("%$marker%"),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }
}

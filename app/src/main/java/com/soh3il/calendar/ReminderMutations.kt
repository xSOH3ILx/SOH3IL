package com.soh3il.calendar

import android.content.Context
import android.net.Uri
import com.soh3il.calendar.widget.CalendarAppWidgetProvider
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.model.Reminder
import com.soh3il.core.scheduler.AlarmScheduler
import com.soh3il.core.scheduler.PersistentNotificationHelper
import com.soh3il.core.scheduler.IcsHelper
import java.io.IOException

internal object ReminderMutations {
    suspend fun importIcs(context: Context, db: Soh3ilDatabase, uri: Uri): Result<Pair<Int, Int>> {
        val imported = try {
            IcsHelper.importFromIcsUri(context, uri)
        } catch (error: Exception) {
            if (error !is IOException && error !is IllegalArgumentException && error !is SecurityException) throw error
            return Result.failure(error)
        }
        val existing = db.reminderDao().getAllRemindersSync().map { it.toDomain() }
        var addedCount = 0
        imported.forEach { rem ->
            val isDuplicate = existing.any {
                it.title == rem.title && it.jalaliYear == rem.jalaliYear &&
                    it.jalaliMonth == rem.jalaliMonth && it.jalaliDay == rem.jalaliDay &&
                    it.hour == rem.hour && it.minute == rem.minute
            }
            if (!isDuplicate) {
                val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(rem))
                AlarmScheduler.scheduleReminder(context, rem.copy(id = id))
                addedCount++
            }
        }
        refresh(context)
        return Result.success(imported.size to addedCount)
    }

    suspend fun save(context: Context, db: Soh3ilDatabase, reminder: Reminder) {
        if (reminder.id != 0L) {
            val previous = db.reminderDao().getReminderById(reminder.id)?.toDomain()
            if (previous != null) AlarmScheduler.cancelReminder(context, previous)
            db.reminderDao().updateReminder(ReminderEntity.fromDomain(reminder))
            AlarmScheduler.scheduleReminder(context, reminder)
        } else {
            val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(reminder))
            AlarmScheduler.scheduleReminder(context, reminder.copy(id = id))
        }
        refresh(context)
    }

    suspend fun delete(context: Context, db: Soh3ilDatabase, reminder: Reminder) {
        db.reminderDao().deleteReminderById(reminder.id)
        AlarmScheduler.cancelReminder(context, reminder)
        refresh(context)
    }

    private fun refresh(context: Context) {
        CalendarAppWidgetProvider.requestUpdate(context)
        if (UserPreferences.isPersistentNotificationEnabled(context)) {
            PersistentNotificationHelper.updateNotification(context)
        }
    }
}

package com.soh3il.feature.reminder

import androidx.compose.runtime.saveable.SaverScope
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import org.junit.Assert.*
import org.junit.Test

class ReminderDraftStateTest {
    @Test fun editingKeepsTheStoredDateInsteadOfTheDefaultDate() {
        val reminder = Reminder(id = 17, title = "ویرایش", jalaliYear = 1403, jalaliMonth = 12, jalaliDay = 30, hour = 10, minute = 0)
        val draft = ReminderDraftState(JalaliDate(1405, 8, 23), reminder)
        assertEquals(JalaliDate(1403, 12, 30), draft.selectedDate)
    }

    @Test fun dateTimeTextAndCustomRecurrenceSurviveSaving() {
        val draft = ReminderDraftState(JalaliDate(1403, 12, 30), null).apply {
            title = "پیش‌نویس"
            description = "یادداشت\nچندخطی"
            selectedHour = 23
            selectedMinute = 59
            recurrenceType = RecurrenceType.YEARLY_GREGORIAN
            recurrenceInterval = 4
            customPreReminder = PreReminder(17, PreReminderUnit.HOURS)
            isCustomPreReminderSelected = true
            recurrenceExpanded = true
            preReminderExpanded = true
        }
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(ReminderDraftState.Saver) { scope.save(draft) }!!
        val restored = ReminderDraftState.Saver.restore(saved)!!
        assertEquals(draft.selectedDate, restored.selectedDate)
        assertEquals("پیش‌نویس", restored.title)
        assertEquals("یادداشت\nچندخطی", restored.description)
        assertEquals(23, restored.selectedHour)
        assertEquals(59, restored.selectedMinute)
        assertEquals(RecurrenceType.YEARLY_GREGORIAN, restored.recurrenceType)
        assertEquals(4, restored.recurrenceInterval)
        assertEquals(PreReminder(17, PreReminderUnit.HOURS), restored.customPreReminder)
        assertTrue(restored.isCustomPreReminderSelected)
        assertTrue(restored.recurrenceExpanded)
        assertTrue(restored.preReminderExpanded)
        assertNull(restored.selectedPreReminderMinutes)
    }
}

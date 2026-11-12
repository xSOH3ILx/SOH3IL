package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderVisibilityTest {

    @Test
    fun completedOneOffStaysVisibleOnItsDateOnly() {
        val reminder = Reminder(
            title = "تمام شده",
            jalaliYear = 1404,
            jalaliMonth = 1,
            jalaliDay = 15,
            hour = 8,
            minute = 0,
            isEnabled = false
        )
        assertTrue(reminder.visibleOn(JalaliDate(1404, 1, 15)))
        assertFalse(reminder.visibleOn(JalaliDate(1404, 1, 16)))
        assertFalse(reminder.matchesDate(JalaliDate(1404, 1, 15)))
    }

    @Test
    fun disabledRecurringReminderStaysHidden() {
        val reminder = Reminder(
            title = "خاموش",
            jalaliYear = 1404,
            jalaliMonth = 1,
            jalaliDay = 1,
            hour = 8,
            minute = 0,
            recurrence = RecurrenceRule(type = RecurrenceType.DAILY),
            isEnabled = false
        )
        assertFalse(reminder.visibleOn(JalaliDate(1404, 1, 3)))
    }
}

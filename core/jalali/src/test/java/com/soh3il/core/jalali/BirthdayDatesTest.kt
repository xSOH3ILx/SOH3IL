package com.soh3il.core.jalali

import org.junit.Assert.*
import org.junit.Test

class BirthdayDatesTest {
    @Test fun januaryBirthdayIsProjectedIntoRequestedSolarYear() {
        assertEquals(listOf(DateConverter.fromGregorian(2027, 1, 15).jalali), BirthdayDates.inSolarYear("1990-01-15", 1405))
    }
    @Test fun acceptsYearlessSlashAndPersianArabicDigits() {
        val expected = BirthdayDates.inSolarYear("--01-15", 1405)
        assertEquals(1, expected.size)
        assertEquals(expected, BirthdayDates.inSolarYear("1990/01/15", 1405))
        assertEquals(expected, BirthdayDates.inSolarYear("۱۹۹۰/۰۱/۱۵", 1405))
        assertEquals(expected, BirthdayDates.inSolarYear("١٩٩٠-٠١-١٥", 1405))
    }
    @Test fun includesBothGregorianOccurrencesAtSolarYearBoundary() {
        val actual = BirthdayDates.inSolarYear("--03-20", 1403)
        assertEquals(listOf(JalaliDate(1403, 1, 1), JalaliDate(1403, 12, 30)), actual)
    }
    @Test fun leapDayIsNotMovedToADifferentBirthday() {
        assertEquals(emptyList<JalaliDate>(), BirthdayDates.inSolarYear("--02-29", 1405))
        assertEquals(listOf(DateConverter.fromGregorian(2024, 2, 29).jalali), BirthdayDates.inSolarYear("--02-29", 1402))
    }
    @Test fun invalidDatesAreIgnoredWithoutGuessingAnotherCalendar() {
        for (raw in listOf("", "invalid", "1990-13-02", "1990-02-30", "2025-02-29", "prefix1990-01-15")) {
            assertTrue(raw, BirthdayDates.inSolarYear(raw, 1405).isEmpty())
        }
    }
}

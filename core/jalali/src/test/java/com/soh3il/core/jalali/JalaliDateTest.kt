package com.soh3il.core.jalali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateTest {

    @Test
    fun testLeapYears() {
        assertTrue("1399 باید سال کبیسه باشد", JalaliDate.isLeap(1399))
        assertTrue("1403 باید سال کبیسه باشد", JalaliDate.isLeap(1403))
        assertTrue("1408 باید سال کبیسه باشد", JalaliDate.isLeap(1408))

        assertFalse("1404 نباید سال کبیسه باشد", JalaliDate.isLeap(1404))
        assertFalse("1405 نباید سال کبیسه باشد", JalaliDate.isLeap(1405))
        assertFalse("1406 نباید سال کبیسه باشد", JalaliDate.isLeap(1406))
    }

    @Test
    fun testDaysInMonth() {
        // Months 1 to 6 must have 31 days
        for (m in 1..6) {
            assertEquals("ماه $m باید ۳۱ روز باشد", 31, JalaliDate.getDaysInMonth(1405, m))
        }

        // Months 7 to 11 must have 30 days
        for (m in 7..11) {
            assertEquals("ماه $m باید ۳۰ روز باشد", 30, JalaliDate.getDaysInMonth(1405, m))
        }

        // Month 12 in normal year has 29 days, in leap year has 30 days
        assertEquals("اسفند ۱۴۰۵ باید ۲۹ روز باشد", 29, JalaliDate.getDaysInMonth(1405, 12))
        assertEquals("اسفند ۱۴۰۳ باید ۳۰ روز باشد (کبیسه)", 30, JalaliDate.getDaysInMonth(1403, 12))
    }

    @Test
    fun testRoundtripEpochConversion() {
        val original = JalaliDate(1405, 1, 13)
        val epoch = original.toEpochMillis(12, 0, 0)
        val reconstructed = JalaliDate.fromEpochMillis(epoch)

        assertEquals(original.year, reconstructed.year)
        assertEquals(original.month, reconstructed.month)
        assertEquals(original.day, reconstructed.day)
    }

    @Test
    fun testDayOfWeekNames() {
        // 1403-01-01 was Wednesday (March 20, 2024 -> Chaharshanbeh = 5)
        val date1403 = JalaliDate(1403, 1, 1)
        assertEquals(5, date1403.dayOfWeek)
        assertEquals("چهارشنبه", date1403.dayOfWeekNameFa)

        // 1403-01-04 was Saturday (March 23, 2024 -> Shanbeh = 1)
        val date1403Sat = JalaliDate(1403, 1, 4)
        assertEquals(1, date1403Sat.dayOfWeek)
        assertEquals("شنبه", date1403Sat.dayOfWeekNameFa)

        // 1403-01-10 was Friday (March 29, 2024 -> Jomeh = 7)
        val date1403Fri = JalaliDate(1403, 1, 10)
        assertEquals(7, date1403Fri.dayOfWeek)
        assertEquals("جمعه", date1403Fri.dayOfWeekNameFa)
    }

    @Test
    fun testPlusMonths() {
        val start = JalaliDate(1405, 1, 31)
        // 6 months later is Mehr (which only has 30 days) -> should snap to 30
        val inMehr = start.plusMonths(6)
        assertEquals(1405, inMehr.year)
        assertEquals(7, inMehr.month)
        assertEquals(30, inMehr.day)
    }
}

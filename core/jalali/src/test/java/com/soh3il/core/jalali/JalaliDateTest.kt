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

    @Test
    fun testEsfandLeapYearBoundaries() {
        // 1403: Leap (Esfand has 30 days)
        assertTrue(JalaliDate.isLeap(1403))
        assertEquals(30, JalaliDate.getDaysInMonth(1403, 12))
        val esfand30_1403 = JalaliDate(1403, 12, 30)
        assertEquals(1, esfand30_1403.plusDays(1).day)
        assertEquals(1, esfand30_1403.plusDays(1).month)
        assertEquals(1404, esfand30_1403.plusDays(1).year)

        // 1404: Non-Leap (Esfand has 29 days)
        assertFalse(JalaliDate.isLeap(1404))
        assertEquals(29, JalaliDate.getDaysInMonth(1404, 12))
        val esfand29_1404 = JalaliDate(1404, 12, 29)
        assertEquals(1, esfand29_1404.plusDays(1).day)
        assertEquals(1, esfand29_1404.plusDays(1).month)
        assertEquals(1405, esfand29_1404.plusDays(1).year)

        // 1407: Non-Leap (Esfand has 29 days)
        assertFalse(JalaliDate.isLeap(1407))
        assertEquals(29, JalaliDate.getDaysInMonth(1407, 12))

        // 1408: Leap (Esfand has 30 days)
        assertTrue(JalaliDate.isLeap(1408))
        assertEquals(30, JalaliDate.getDaysInMonth(1408, 12))

        // 1436: Leap (Official 5-year cycle)
        assertTrue(JalaliDate.isLeap(1436))
        assertEquals(30, JalaliDate.getDaysInMonth(1436, 12))
    }

    @Test
    fun testCanonicalEngineModernEraBoundary() {
        assertTrue(JalaliDate.isLeap(1498))
        assertEquals(30, JalaliDate.getDaysInMonth(1498, 12))

        val endOfLookupEra = JalaliDate(1498, 12, 30)
        val firstAfterLookupEra = endOfLookupEra.plusDays(1)
        assertEquals(1499, firstAfterLookupEra.year)
        assertEquals(1, firstAfterLookupEra.month)
        assertEquals(1, firstAfterLookupEra.day)
    }
}

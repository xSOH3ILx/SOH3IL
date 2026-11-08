package com.soh3il.core.jalali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianHolidaysTest {

    @Test
    fun testNowruzHolidaysInFarvardin() {
        val occasions = IranianHolidays.getOccasionsForMonth(1404, 1)
        val holidayTitles = occasions.filter { it.isHoliday }.map { it.titleFa }

        assertTrue(holidayTitles.any { it.contains("نوروز") })
        assertTrue(holidayTitles.any { it.contains("۱۲ فروردین") || it.contains("جمهوری اسلامی") })
        assertTrue(holidayTitles.any { it.contains("سیزده‌بدر") || it.contains("طبیعت") })
    }

    @Test
    fun testKhordadHolidays() {
        val occasions = IranianHolidays.getOccasionsForMonth(1404, 3)
        val holidayTitles = occasions.filter { it.isHoliday }.map { it.titleFa }

        assertTrue(holidayTitles.any { it.contains("۱۴ خرداد") || it.contains("امام خمینی") })
        assertTrue(holidayTitles.any { it.contains("۱۵ خرداد") })
    }

    @Test
    fun testEsfandOilNationalizationAndLeapYear() {
        // 1403 was a leap year
        val occasions1403 = IranianHolidays.getOccasionsForMonth(1403, 12)
        assertTrue("Esfand 29 must be holiday", occasions1403.any { it.jalaliDate == "1403-12-29" && it.isHoliday })
        assertTrue("Esfand 30 must be holiday in leap year 1403", occasions1403.any { it.jalaliDate == "1403-12-30" && it.isHoliday })

        // 1404 is not a leap year
        val occasions1404 = IranianHolidays.getOccasionsForMonth(1404, 12)
        assertTrue("Esfand 29 must be holiday", occasions1404.any { it.jalaliDate == "1404-12-29" && it.isHoliday })
        assertFalse("Esfand 30 should not exist in non-leap year 1404", occasions1404.any { it.jalaliDate == "1404-12-30" })
    }

    @Test
    fun testLunarHolidays1404() {
        // Farvardin 1404 has Eid al-Fitr (11 and 12)
        val occasionsFarvardin = IranianHolidays.getOccasionsForMonth(1404, 1)
        assertTrue(occasionsFarvardin.any { it.jalaliDate == "1404-01-11" && it.titleFa.contains("فطر") })

        // Tir 1404 has Tasua & Ashura (14 and 15)
        val occasionsTir = IranianHolidays.getOccasionsForMonth(1404, 4)
        assertTrue(occasionsTir.any { it.jalaliDate == "1404-04-14" && it.titleFa.contains("تاسوعا") })
        assertTrue(occasionsTir.any { it.jalaliDate == "1404-04-15" && it.titleFa.contains("عاشورا") })
    }

    @Test
    fun testHolidaysOrderIsChronological() {
        val occasions = IranianHolidays.getOccasionsForMonth(1404, 1)
        val days = occasions.map { it.jalaliDate.split("-").last().toInt() }
        val sortedDays = days.sorted()
        assertEquals(sortedDays, days)
    }
}

package com.soh3il.core.jalali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianHolidaysTest {

    @Test
    fun testNowruzHolidaysInFarvardin() {
        val occasions = IranianHolidays.getOccasionsForMonth(1404, 1)
        val holidays = occasions.filter { it.isHoliday }
        val holidayTitles = holidays.map { it.titleFa }

        assertTrue(holidayTitles.any { it.contains("نوروز") })
        assertTrue(holidayTitles.any { it.contains("۱۲ فروردین") || it.contains("جمهوری اسلامی") })
        assertTrue(holidayTitles.any { it.contains("سیزده‌بدر") || it.contains("طبیعت") })

        // Check that 1st to 4th Farvardin are holidays
        assertTrue(holidays.any { it.jalaliDate == "1404-01-01" })
        assertTrue(holidays.any { it.jalaliDate == "1404-01-02" })
        assertTrue(holidays.any { it.jalaliDate == "1404-01-03" })
        assertTrue(holidays.any { it.jalaliDate == "1404-01-04" })
    }

    @Test
    fun testCulturalOccasionsAreNotMarkedAsHolidays() {
        // Shab-e Yalda (30 Azar) must be present but NOT a holiday
        val occasionsAzar = IranianHolidays.getOccasionsForMonth(1404, 9)
        val yalda = occasionsAzar.firstOrNull { it.jalaliDate == "1404-09-30" }
        assertTrue("شب یلدا باید در مناسبت‌ها باشد", yalda != null && yalda.titleFa.contains("یلدا"))
        assertFalse("شب یلدا نباید تعطیل رسمی باشد", yalda!!.isHoliday)

        // Cyrus Day (7 Aban) must be present but NOT a holiday
        val occasionsAban = IranianHolidays.getOccasionsForMonth(1404, 8)
        val cyrus = occasionsAban.firstOrNull { it.jalaliDate == "1404-08-07" }
        assertTrue("بزرگداشت کوروش باید در مناسبت‌ها باشد", cyrus != null && cyrus.titleFa.contains("کوروش"))
        assertFalse("بزرگداشت کوروش نباید تعطیل رسمی باشد", cyrus!!.isHoliday)

        // Teacher's Day (12 Ordibehesht) must be present but NOT a holiday
        val occasionsOrdibehesht = IranianHolidays.getOccasionsForMonth(1404, 2)
        val teacherDay = occasionsOrdibehesht.firstOrNull { it.jalaliDate == "1404-02-12" }
        assertTrue("روز معلم باید در مناسبت‌ها باشد", teacherDay != null && teacherDay.titleFa.contains("معلم"))
        assertFalse("روز معلم نباید تعطیل رسمی باشد", teacherDay!!.isHoliday)
    }

    @Test
    fun testKhordadHolidays() {
        val occasions = IranianHolidays.getOccasionsForMonth(1404, 3)
        val holidayTitles = occasions.filter { it.isHoliday }.map { it.titleFa }

        assertTrue(holidayTitles.any { it.contains("۱۴ خرداد") || it.contains("امام خمینی") })
        assertTrue(holidayTitles.any { it.contains("۱۵ خرداد") })
        assertTrue(holidayTitles.any { it.contains("قربان") })
        assertTrue(holidayTitles.any { it.contains("غدیر") })
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
    fun testLunarHolidays1404ExactTehranUniversityCalendar() {
        // Farvardin 1404: Eid al-Fitr is 11 and 12
        val occasionsFarvardin = IranianHolidays.getOccasionsForMonth(1404, 1)
        assertTrue(occasionsFarvardin.any { it.jalaliDate == "1404-01-11" && it.isHoliday && it.titleFa.contains("فطر") })
        assertTrue(occasionsFarvardin.any { it.jalaliDate == "1404-01-12" && it.isHoliday })

        // Ordibehesht 1404: Imam Sadiq martyrdom is on 4th of Ordibehesht
        val occasionsOrdibehesht = IranianHolidays.getOccasionsForMonth(1404, 2)
        assertTrue(occasionsOrdibehesht.any { it.jalaliDate == "1404-02-04" && it.isHoliday && it.titleFa.contains("صادق") })

        // Tir 1404: Tasua (14) & Ashura (15)
        val occasionsTir = IranianHolidays.getOccasionsForMonth(1404, 4)
        assertTrue(occasionsTir.any { it.jalaliDate == "1404-04-14" && it.isHoliday && it.titleFa.contains("تاسوعا") })
        assertTrue(occasionsTir.any { it.jalaliDate == "1404-04-15" && it.isHoliday && it.titleFa.contains("عاشورا") })

        // Mordad 1404: Arbaeen is 23 Mordad, Prophet demise is 31 Mordad
        val occasionsMordad = IranianHolidays.getOccasionsForMonth(1404, 5)
        assertTrue(occasionsMordad.any { it.jalaliDate == "1404-05-23" && it.isHoliday && it.titleFa.contains("اربعین") })
        assertTrue(occasionsMordad.any { it.jalaliDate == "1404-05-31" && it.isHoliday && it.titleFa.contains("رحلت") })

        // Shahrivar 1404: Imam Reza martyrdom (2), Imam Askari (10), Prophet birthday (19)
        val occasionsShahrivar = IranianHolidays.getOccasionsForMonth(1404, 6)
        assertTrue(occasionsShahrivar.any { it.jalaliDate == "1404-06-02" && it.isHoliday && it.titleFa.contains("رضا") })
        assertTrue(occasionsShahrivar.any { it.jalaliDate == "1404-06-10" && it.isHoliday && it.titleFa.contains("عسکری") })
        assertTrue(occasionsShahrivar.any { it.jalaliDate == "1404-06-19" && it.isHoliday && it.titleFa.contains("میلاد") })
    }

    @Test
    fun testHolidaysOrderIsChronological() {
        val occasions = IranianHolidays.getOccasionsForMonth(1404, 1)
        val days = occasions.map { it.jalaliDate.split("-").last().toInt() }
        val sortedDays = days.sorted()
        assertEquals(sortedDays, days)
    }
}

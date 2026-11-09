package com.soh3il.core.jalali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class DateConverterTest {

    @Test
    fun testConversionFromJalaliToGregorianAndHijri() {
        // 1 Farvardin 1404
        val result = DateConverter.fromJalali(1404, 1, 1)

        assertEquals(LocalDate.of(2025, 3, 21), result.gregorian)
        assertEquals("مار", result.yearAnimal)
        assertNotNull(result.hijriYear)
        assertEquals("جمعه", result.dayOfWeekNameFa)
        assertEquals("Friday", result.dayOfWeekNameEn)
    }

    @Test
    fun testConversionFromGregorian() {
        val result = DateConverter.fromGregorian(2025, 3, 21)
        assertEquals(1404, result.jalali.year)
        assertEquals(1, result.jalali.month)
        assertEquals(1, result.jalali.day)
    }

    @Test
    fun testYearAnimalZodiacCycle() {
        assertEquals("خرگوش", DateConverter.fromJalali(1402, 1, 1).yearAnimal)
        assertEquals("نهنگ", DateConverter.fromJalali(1403, 1, 1).yearAnimal)
        assertEquals("مار", DateConverter.fromJalali(1404, 1, 1).yearAnimal)
        assertEquals("اسب", DateConverter.fromJalali(1405, 1, 1).yearAnimal)
    }

    @Test
    fun testLeapYearBoundaryTransitions() {
        // 1403 Esfand 30th (Leap Year)
        val conv1403 = DateConverter.fromJalali(1403, 12, 30)
        assertEquals(LocalDate.of(2025, 3, 20), conv1403.gregorian)
        val roundtrip1403 = DateConverter.fromGregorian(2025, 3, 20)
        assertEquals(1403, roundtrip1403.jalali.year)
        assertEquals(12, roundtrip1403.jalali.month)
        assertEquals(30, roundtrip1403.jalali.day)

        // 1404 Esfand 29th (Non-Leap Year)
        val conv1404 = DateConverter.fromJalali(1404, 12, 29)
        assertEquals(LocalDate.of(2026, 3, 20), conv1404.gregorian)
        val roundtrip1404 = DateConverter.fromGregorian(2026, 3, 20)
        assertEquals(1404, roundtrip1404.jalali.year)
        assertEquals(12, roundtrip1404.jalali.month)
        assertEquals(29, roundtrip1404.jalali.day)

        // 1407 Esfand 29th (Non-Leap Year, day before Nowruz 1408)
        val conv1407 = DateConverter.fromJalali(1407, 12, 29)
        assertEquals(LocalDate.of(2029, 3, 19), conv1407.gregorian)
        val roundtrip1407 = DateConverter.fromGregorian(2029, 3, 19)
        assertEquals(1407, roundtrip1407.jalali.year)
        assertEquals(12, roundtrip1407.jalali.month)
        assertEquals(29, roundtrip1407.jalali.day)

        // 1408 Esfand 30th (Leap Year)
        val conv1408 = DateConverter.fromJalali(1408, 12, 30)
        assertEquals(LocalDate.of(2030, 3, 20), conv1408.gregorian)
        val roundtrip1408 = DateConverter.fromGregorian(2030, 3, 20)
        assertEquals(1408, roundtrip1408.jalali.year)
        assertEquals(12, roundtrip1408.jalali.month)
        assertEquals(30, roundtrip1408.jalali.day)

        // 1436 Esfand 30th (Astronomical 5-year leap cycle)
        val conv1436 = DateConverter.fromJalali(1436, 12, 30)
        assertEquals(LocalDate.of(2058, 3, 20), conv1436.gregorian)
        val roundtrip1436 = DateConverter.fromGregorian(2058, 3, 20)
        assertEquals(1436, roundtrip1436.jalali.year)
        assertEquals(12, roundtrip1436.jalali.month)
        assertEquals(30, roundtrip1436.jalali.day)
    }
}

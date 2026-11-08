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
}

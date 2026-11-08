package com.soh3il.core.jalali

import com.soh3il.core.jalali.engine.civilFromJdn
import com.soh3il.core.jalali.engine.jdnFromCivil
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * مبدل جامع و سه‌گانه تقویم‌های خورشیدی (جلالی)، میلادی (گرگوری) و قمری (هجری قمری)
 * به همراه تحلیل نجومی و گاه‌شماری (حیوان سال، کبیسه، روز هفته، اختلاف با امروز).
 */
object DateConverter {

    private val YEAR_ANIMALS = listOf(
        "مار", "اسب", "گوسفند", "میمون", "مرغ", "سگ", "خوک", "موش", "گاو", "پلنگ", "خرگوش", "نهنگ"
    )

    data class ConversionResult(
        val jalali: JalaliDate,
        val gregorian: LocalDate,
        val hijriYear: Int,
        val hijriMonth: Int,
        val hijriDay: Int,
        val dayOfWeekNameFa: String,
        val dayOfWeekNameEn: String,
        val isJalaliLeap: Boolean,
        val yearAnimal: String,
        val diffDaysFromToday: Long,
        val relativeDescriptionFa: String
    )

    /**
     * تبدیل از تاریخ شمسی
     */
    fun fromJalali(year: Int, month: Int, day: Int): ConversionResult {
        val jDate = JalaliDate(year, month, day)
        val jdn = jDate.toJdn()
        val (gy, gm, gd) = civilFromJdn(jdn)
        val gDate = LocalDate.of(gy, gm, gd)
        return buildResult(jDate, gDate)
    }

    /**
     * تبدیل از تاریخ میلادی
     */
    fun fromGregorian(year: Int, month: Int, day: Int): ConversionResult {
        val gDate = LocalDate.of(year, month, day)
        val jdn = jdnFromCivil(year, month, day)
        val jDate = JalaliDate.fromJdn(jdn)
        return buildResult(jDate, gDate)
    }

    /**
     * تبدیل از تاریخ هجری قمری
     */
    fun fromHijri(year: Int, month: Int, day: Int): ConversionResult {
        val hDate = HijrahDate.of(year, month, day)
        val gDate = LocalDate.from(hDate)
        val jdn = jdnFromCivil(gDate.year, gDate.monthValue, gDate.dayOfMonth)
        val jDate = JalaliDate.fromJdn(jdn)
        return buildResult(jDate, gDate)
    }

    private fun buildResult(jDate: JalaliDate, gDate: LocalDate): ConversionResult {
        val hDate = runCatching { HijrahDate.from(gDate) }.getOrNull()
        val hYear = hDate?.get(ChronoField.YEAR) ?: 0
        val hMonth = hDate?.get(ChronoField.MONTH_OF_YEAR) ?: 0
        val hDay = hDate?.get(ChronoField.DAY_OF_MONTH) ?: 0

        val todayG = LocalDate.now()
        val diffDays = java.time.temporal.ChronoUnit.DAYS.between(todayG, gDate)

        val relativeFa = when {
            diffDays == 0L -> "امروز"
            diffDays == 1L -> "فردا"
            diffDays == -1L -> "دیروز"
            diffDays > 0 -> "$diffDays روز دیگر"
            else -> "${-diffDays} روز پیش"
        }

        val animal = YEAR_ANIMALS[((jDate.year % 12) + 12) % 12]

        return ConversionResult(
            jalali = jDate,
            gregorian = gDate,
            hijriYear = hYear,
            hijriMonth = hMonth,
            hijriDay = hDay,
            dayOfWeekNameFa = jDate.dayOfWeekNameFa,
            dayOfWeekNameEn = gDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
            isJalaliLeap = jDate.isLeapYear,
            yearAnimal = animal,
            diffDaysFromToday = diffDays,
            relativeDescriptionFa = relativeFa
        )
    }
}
